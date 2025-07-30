from copy import deepcopy
from datetime import datetime, time, timedelta
from typing import Any, Type

from django import forms
from django.conf import settings
from django.contrib.auth.decorators import login_required as django_login_required
from django.db import connection
from django.db.models import Model, Prefetch, Q, QuerySet
from django.db.models.signals import post_save
from django.forms import Form, ModelForm, TextInput
from django.http import HttpRequest, HttpResponseNotFound
from django.http.response import HttpResponse as HttpResponse
from django.shortcuts import redirect, render
from django.template.loader import render_to_string
from django.utils import timezone
from django.utils.safestring import mark_safe
from django.views import View

from demschooltools.form_renderer import BootstrapFormRenderer
from dst.models import (
    Chapter,
    Entry,
    ManualChange,
    Organization,
    Section,
    User,
    UserRole,
)
from dst.org_config import OrgConfig, get_org_config
from dst.pdf_utils import (
    create_pdf_response,
    render_html_to_pdf,
    render_multiple_html_to_pdf,
)


def login_required():
    return django_login_required(login_url="/login")


class DstHttpRequest(HttpRequest):
    org: Organization


def render_main_template(
    request: DstHttpRequest,
    content: str,
    title: str,
    selected_button: str | None = None,
):
    return render(
        request,
        "main.html",
        {
            "title": title,
            "menu": "manual",
            "selectedBtn": selected_button,
            "content": mark_safe(content),
            "current_username": request.user.email
            if request.user.is_authenticated
            else None,
            "is_user_logged_in": request.user.is_authenticated,
            "org_config": get_org_config(request.org),
            "rollbar_environment": settings.ROLLBAR_ENVIRONMENT,
        },
    )


def render_main_template_to_string(*args, **kwargs) -> str:
    return render_main_template(*args, **kwargs).content.decode("utf-8")


@login_required()
def view_manual(request: DstHttpRequest):
    chapters = chapter_with_entries(request.org)

    org_config = get_org_config(request.org)

    return render_main_template(
        request,
        render_to_string(
            "view_manual.html",
            {
                "chapters": chapters,
                "current_date": datetime.now().strftime("%Y-%m-%d"),
                "org_config": org_config,
            },
        ),
        f"{request.org.short_name} {org_config.str_manual_title}",
        selected_button="toc",
    )


class ManualChangesForm(Form):
    begin_date = forms.DateField()
    end_date = forms.DateField(required=False)


@login_required()
def view_manual_changes(request: DstHttpRequest):
    form = ManualChangesForm(request.GET)

    end_date = timezone.localtime().replace(hour=0, minute=0, second=0, microsecond=0)
    begin_date = end_date - timedelta(days=7)
    end_date += timedelta(days=1, microseconds=-1)

    if form.is_valid():
        begin_date = timezone.make_aware(
            datetime.combine(form.cleaned_data["begin_date"], time())
        )
        if form.cleaned_data["end_date"]:
            end_date = timezone.make_aware(
                datetime.combine(form.cleaned_data["end_date"], time())
            ) + timedelta(days=1, microseconds=-1)
    print(begin_date, end_date)

    changes = list(
        ManualChange.objects.filter(
            entry__section__chapter__organization=request.org,
        )
        .filter(
            Q(date_entered__gte=begin_date, date_entered__lte=end_date)
            | Q(
                effective_date__gte=begin_date.date(),
                effective_date__lte=end_date.date(),
            )
        )
        .prefetch_related("user")
    )

    changes.sort(
        key=lambda change: (
            change.date_entered.date(),
            change.new_num or change.old_num,
            change.date_entered,
        )
    )

    org_config = get_org_config(request.org)

    return render_main_template(
        request,
        render_to_string(
            "view_manual_changes.html",
            {
                "begin_date": begin_date.strftime("%Y-%m-%d"),
                "end_date": end_date.strftime("%Y-%m-%d"),
                "changes": changes,
                "org_config": org_config,
            },
        ),
        org_config.str_manual_title + " changes",
        "manual_changes",
    )


def chapter_with_entries(org: Organization, include_deleted=False) -> QuerySet[Chapter]:
    manager = Chapter.all_objects if include_deleted else Chapter.objects
    return manager.filter(organization=org).prefetch_related(
        Prefetch(
            "sections",
            Section.objects.prefetch_related(
                Prefetch(
                    "entries",
                    Entry.objects.prefetch_related(
                        Prefetch(
                            "changes", ManualChange.objects.order_by("date_entered")
                        )
                    ),
                )
            ),
        )
    )


@login_required()
def view_chapter(request: DstHttpRequest, chapter_id: int):
    chapter = (
        chapter_with_entries(request.org, include_deleted=True)
        .filter(id=chapter_id)
        .first()
    )
    if chapter is None:
        return HttpResponseNotFound()

    return render_main_template(
        request,
        render_to_string(
            "view_chapter.html",
            {
                "chapter": chapter,
                "org_config": get_org_config(request.org),
            },
        ),
        f"{chapter.num} {chapter.title}",
    )


def on_manual_change(*args, **kwargs):
    with connection.cursor() as cursor:
        cursor.execute("REFRESH MATERIALIZED VIEW entry_index WITH DATA")


post_save.connect(on_manual_change, sender=Chapter)
post_save.connect(on_manual_change, sender=Section)
post_save.connect(on_manual_change, sender=Entry)


class CreateUpdateView(View):
    form_class: Type[ModelForm] = ModelForm
    template_name = ""
    object_name = ""
    view_url_base = ""
    role = ""  # value from UserRole, e.g. UserRole.EDIT_MANUAL

    def __init__(self, **kwargs: Any) -> None:
        super().__init__(**kwargs)
        self.existing_object = None

    def get_success_url(self, instance: Model):
        raise NotImplementedError("get_success_url is not overriden")

    def get_initial_for_create(self, request: DstHttpRequest, **kwargs):
        return {}

    def pre_save(self, request: DstHttpRequest, instance: Model):
        pass

    def post_save(self, request: DstHttpRequest, instance: Model):
        pass

    def get_form_context(
        self, request: DstHttpRequest, form: ModelForm, context: dict
    ) -> dict:
        return context

    def render(self, request: DstHttpRequest, form: ModelForm):
        return render_main_template(
            request,
            render_to_string(
                self.template_name,
                self.get_form_context(
                    request,
                    form,
                    {
                        "org_config": get_org_config(request.org),
                        "form": form,
                        "is_new": self.existing_object is None,
                    },
                ),
                request=request,
            ),
            f"Edit a {self.object_name}"
            if self.existing_object
            else f"Add new {self.object_name}",
        )

    def load_object(self, request: DstHttpRequest, object_id: int):
        assert self.form_class._meta.model is not None, (
            "Your form must include a model in class Meta"
        )
        # Use all_objects so that this works even for deleted items
        self.existing_object = self.form_class._meta.model.all_objects.get(id=object_id)  # type: ignore

    def post(self, request: DstHttpRequest, **kwargs):
        # Pass a copy into the form so that self.existing_object remains unchanged
        self.form = self.form_class(
            request.POST, instance=deepcopy(self.existing_object)
        )
        if self.form.is_valid():
            new_object = self.form.save(commit=False)
            self.pre_save(request, new_object)
            new_object.save()
            self.post_save(request, new_object)

            return redirect(self.get_success_url(new_object))

        return self.render(request, self.form)

    def get(self, request: DstHttpRequest, **kwargs):
        if self.existing_object:
            self.form = self.form_class(instance=deepcopy(self.existing_object))
        else:
            self.form = self.form_class(
                initial=self.get_initial_for_create(request, **kwargs)
            )
        return self.render(request, self.form)

    def can_edit(self, request: DstHttpRequest, instance: Model) -> bool:
        return True

    def dispatch(
        self,
        request: DstHttpRequest,
        object_id: int | None = None,
        **kwargs: Any,
    ) -> HttpResponse:
        if not request.user.is_authenticated:
            raise PermissionError("You need to be logged in.")
        assert isinstance(request.user, User)
        assert self.role
        if not request.user.hasRole(self.role):
            raise PermissionError("You don't have privileges to access this item")

        if object_id is not None:
            self.load_object(request, object_id)
            assert self.existing_object is not None

            if not self.can_edit(request, self.existing_object):
                raise PermissionError()

        return super().dispatch(request, object_id=object_id, **kwargs)


class ChapterForm(ModelForm):
    default_renderer = BootstrapFormRenderer

    class Meta:
        model = Chapter
        fields = ["num", "title", "deleted"]
        labels = {
            "num": "Number",
            "deleted": "Check this to delete",
        }
        widgets = {"num": TextInput(), "title": TextInput()}


class CreateUpdateChapter(CreateUpdateView):
    form_class = ChapterForm
    template_name = "edit_chapter.html"
    object_name = "chapter"
    role = UserRole.EDIT_MANUAL

    def get_success_url(self, instance):
        assert isinstance(instance, Chapter)
        return f"/viewChapter/{instance.id}"

    def can_edit(self, request: DstHttpRequest, instance: Model):
        assert isinstance(instance, Chapter)
        return instance.organization_id == request.org.id

    def pre_save(self, request: DstHttpRequest, instance: Model):
        assert isinstance(instance, Chapter)
        if instance.organization_id is None:
            instance.organization = request.org


class SectionForm(ModelForm):
    default_renderer = BootstrapFormRenderer

    class Meta:
        model = Section
        fields = ["chapter", "num", "title", "deleted"]
        labels = {
            "num": "Number",
            "deleted": "Check this to delete",
        }
        widgets = {"num": TextInput(), "title": TextInput()}


class CreateUpdateSection(CreateUpdateView):
    form_class = SectionForm
    template_name = "edit_section.html"
    object_name = "section"
    role = UserRole.EDIT_MANUAL

    def __init__(self, **kwargs: Any) -> None:
        super().__init__(**kwargs)
        self.chapter = None

    def get_success_url(self, instance):
        assert isinstance(instance, Section)
        return f"/viewChapter/{instance.chapter_id}#section_{instance.id}"

    def can_edit(self, request: DstHttpRequest, instance: Model):
        assert isinstance(instance, Section)
        return instance.chapter.organization_id == request.org.id

    def get_initial_for_create(self, request: DstHttpRequest, **kwargs):
        self.chapter = Chapter.objects.get(
            organization=request.org, id=kwargs.get("chapter_id")
        )
        return {"chapter": self.chapter}

    def get_form_context(
        self, request: DstHttpRequest, form: ModelForm, context: dict
    ) -> dict:
        chapter = self.chapter or form.instance.chapter
        context["chapter_num"] = chapter.num
        return context


class EntryForm(ModelForm):
    default_renderer = BootstrapFormRenderer

    effective_date = forms.DateField(initial=timezone.now().date())

    class Meta:
        model = Entry
        fields = ["section", "num", "title", "deleted", "content"]
        labels = {
            "num": "Number",
            "deleted": "Check this to delete",
        }
        widgets = {"num": TextInput(), "title": TextInput()}

    def clean_content(self):
        content = self.cleaned_data.get("content", "")
        return content.replace("\r\n", "\n").replace("\r", "\n")


def get_manual_change_for_entry(
    user: User, entry_form: EntryForm, old_instance: Entry | None, new_instance: Entry
) -> ManualChange | None:
    common_data = dict(
        date_entered=timezone.localtime(),
        effective_date=entry_form.cleaned_data["effective_date"],
        entry=new_instance,
        new_content=new_instance.content,
        new_num=new_instance.number(),
        new_title=new_instance.title,
        user=user,
    )

    if old_instance is None:
        return ManualChange(was_created=True, **common_data)
    else:
        common_data.update(
            old_content=old_instance.content,
            old_num=old_instance.number(),
            old_title=old_instance.title,
        )
        if new_instance.deleted and not old_instance.deleted:
            return ManualChange(was_deleted=True, **common_data)
        else:
            if (
                common_data["old_content"] != common_data["new_content"]
                or common_data["old_title"] != common_data["new_title"]
                or common_data["old_num"] != common_data["new_num"]
            ):
                return ManualChange(**common_data)

    return None


class CreateUpdateEntry(CreateUpdateView):
    form_class = EntryForm
    template_name = "edit_entry.html"
    object_name = "entry"
    role = UserRole.EDIT_MANUAL

    def __init__(self, **kwargs: Any) -> None:
        super().__init__(**kwargs)
        self.section = None

    def get_success_url(self, instance):
        assert isinstance(instance, Entry)
        return f"/viewChapter/{instance.section.chapter_id}#entry_{instance.id}"

    def can_edit(self, request: DstHttpRequest, instance: Model):
        assert isinstance(instance, Entry)
        return instance.section.chapter.organization_id == request.org.id

    def get_initial_for_create(self, request: DstHttpRequest, **kwargs):
        self.section = Section.objects.get(
            chapter__organization=request.org, id=kwargs.get("section_id")
        )
        return {"section": self.section}

    def get_form_context(
        self, request: DstHttpRequest, form: ModelForm, context: dict
    ) -> dict:
        section = self.section or form.instance.section
        context["section_number"] = section.number()
        return context

    def post_save(self, request: DstHttpRequest, instance: Model):
        assert isinstance(instance, Entry)
        assert self.existing_object is None or isinstance(self.existing_object, Entry)
        assert isinstance(self.form, EntryForm)
        assert isinstance(request.user, User)

        if new_change := get_manual_change_for_entry(
            request.user, self.form, self.existing_object, instance
        ):
            new_change.save()


@login_required()
def print_manual(request: DstHttpRequest):
    """
    Display the print manual page with download links for PDF versions.
    """
    chapters = chapter_with_entries(request.org)
    org_config = get_org_config(request.org)

    return render_main_template(
        request,
        render_to_string(
            "print_manual.html",
            {
                "chapters": chapters,
                "org_config": org_config,
            },
        ),
        f"Download {org_config.str_manual_title} as a PDF",
        selected_button="manual_print",
    )


@login_required()
def search_manual(request: DstHttpRequest):
    search_string = request.GET.get("searchString", "")

    entries_with_headlines = []
    if search_string:
        entries_with_headlines = list(
            Entry.objects.filter(section__chapter__organization=request.org)
            .extra(
                where=[
                    "EXISTS (SELECT 1 FROM entry_index ei WHERE ei.id = entry.id AND ei.document @@ plainto_tsquery(%s))"
                ],
                params=[search_string],
                select={
                    "headline": "ts_headline(entry.content, plainto_tsquery(%s), 'MaxFragments=5')",
                    "search_rank": "ts_rank((SELECT ei.document FROM entry_index ei WHERE ei.id = entry.id), plainto_tsquery(%s), 0)",
                },
                select_params=[search_string, search_string],
                order_by=["-search_rank"],
            )
            .select_related("section__chapter")
        )

    org_config = get_org_config(request.org)

    return render_main_template(
        request,
        render_to_string(
            "manual_search.html",
            {
                "searchString": search_string,
                "entries": entries_with_headlines,
                "org_config": org_config,
            },
        ),
        f"{org_config.str_manual_title} Search: {search_string}",
    )


@login_required()
def preview_entry(request: DstHttpRequest, object_id: int | None = None):
    existing = None
    if object_id:
        existing = Entry.all_objects.filter(id=object_id).first()

    entry_form = EntryForm(request.POST, instance=deepcopy(existing))
    temp_entry: Entry = entry_form.save(commit=False)
    changes = temp_entry.changes_for_render()

    if new_change := get_manual_change_for_entry(
        request.user, entry_form, existing, temp_entry
    ):
        changes.append(new_change)

    temp_entry.changes_for_render = lambda *args: changes

    return render(
        request,
        "view_entry.html",
        {
            "entry": temp_entry,
            "org_config": get_org_config(request.org),
            "is_preview": True,
        },
    )


def get_chapter_html(
    chapter: Chapter, request: DstHttpRequest, org_config: OrgConfig
) -> str:
    return render_main_template_to_string(
        request,
        render_to_string(
            "view_chapter.html",
            {"chapter": chapter, "org_config": org_config},
        ),
        "",
        "",
    )


@login_required()
def print_manual_chapter(request: DstHttpRequest, chapter_id: int):
    org = request.org
    org_config = get_org_config(org)

    if chapter_id == -1:
        # Render complete manual (TOC + all chapters)
        documents = []

        chapters = chapter_with_entries(org)
        toc_html = render_main_template_to_string(
            request,
            render_to_string(
                "view_manual.html",
                {
                    "chapters": chapters,
                    "current_date": datetime.now().strftime("%Y-%m-%d"),
                    "org_config": org_config,
                },
            ),
            f"{org.short_name} {org_config.str_manual_title}",
            selected_button="toc",
        )
        documents.append(toc_html)

        for chapter in chapters:
            documents.append(get_chapter_html(chapter, request, org_config))

        return create_pdf_response(
            render_multiple_html_to_pdf(documents), f"{org.short_name}_Manual.pdf"
        )

    elif chapter_id == -2:
        # Render TOC only
        chapters = chapter_with_entries(org)
        toc_html = render_main_template_to_string(
            request,
            render_to_string(
                "view_manual.html",
                {
                    "chapters": chapters,
                    "current_date": datetime.now().strftime("%Y-%m-%d"),
                    "org_config": org_config,
                },
            ),
            f"{org.short_name} {org_config.str_manual_title}",
            selected_button="toc",
        )

        return create_pdf_response(
            render_html_to_pdf(toc_html), f"{org.short_name}_Manual_TOC.pdf"
        )

    else:
        # Render specific chapter
        chapter = chapter_with_entries(org).filter(id=chapter_id).first()
        if chapter is None:
            return HttpResponseNotFound()

        pdf_bytes = render_html_to_pdf(get_chapter_html(chapter, request, org_config))
        return create_pdf_response(
            pdf_bytes, f"{org.short_name}_Chapter_{chapter.num}.pdf"
        )
