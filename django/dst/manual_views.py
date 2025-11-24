import re
from copy import deepcopy
from datetime import datetime, time, timedelta
from typing import Any, Type

from django import forms
from django.contrib.auth.decorators import login_required as django_login_required
from django.db.models import CharField, Model, Prefetch, Q, QuerySet, Value
from django.db.models.functions import Concat
from django.db.transaction import atomic
from django.forms import Form, ModelForm, TextInput, ValidationError
from django.http import HttpRequest, HttpResponseNotFound
from django.http.response import HttpResponse as HttpResponse
from django.shortcuts import redirect, render
from django.template.loader import render_to_string
from django.utils import timezone
from django.utils.html import escape
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
from dst.org_config import get_org_config
from dst.pdf_utils import (
    create_pdf_response,
    render_html_to_pdf,
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
        },
    )


def get_html_from_response(response: HttpResponse) -> str:
    return response.content.decode("utf-8")


@login_required()
def view_manual(request: DstHttpRequest):
    chapters = chapters_with_entries(request.org)

    org_config = get_org_config(request.org)

    return render_main_template(
        request,
        render_to_string(
            "view_manual_toc.html",
            {
                "chapters": chapters,
                "org_config": org_config,
                "can_edit": request.user.hasRole(UserRole.EDIT_MANUAL),
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


def chapters_with_entries(
    org: Organization, include_deleted=False
) -> QuerySet[Chapter]:
    manager = Chapter.all_objects if include_deleted else Chapter.objects
    return manager.filter(organization=org).prefetch_related(
        Prefetch(
            "sections",
            Section.objects.prefetch_related(
                Prefetch(
                    "entries",
                    Entry.objects.prefetch_related(
                        Prefetch(
                            "changes",
                            ManualChange.objects.order_by("date_entered").filter(
                                show_date_in_history=True
                            ),
                        )
                    ),
                )
            ),
        )
    )


@login_required()
def view_chapter(request: DstHttpRequest, chapter_id: int):
    chapter = (
        chapters_with_entries(request.org, include_deleted=True)
        .filter(id=chapter_id)
        .first()
    )
    if chapter is None:
        return HttpResponseNotFound()

    return get_chapter_response(chapter, request)


class ModelFormWithOrg(ModelForm):
    def __init__(self, *args, organization=None, **kwargs) -> None:
        assert organization is not None
        super().__init__(*args, **kwargs)


class CreateUpdateView(View):
    form_class: Type[ModelFormWithOrg] = ModelFormWithOrg
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

    @atomic
    def post(self, request: DstHttpRequest, **kwargs):
        if self.existing_object:
            # Pass a copy into the form so that self.existing_object remains unchanged
            new_instance = deepcopy(self.existing_object)
            assert hasattr(new_instance, "deleted")
            # Set deleted here so that form validation can take it into account
            new_instance.deleted = request.POST.get("action") == "delete"
        else:
            new_instance = None

        self.form = self.form_class(
            request.POST, instance=new_instance, organization=request.org
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
            self.form = self.form_class(
                instance=deepcopy(self.existing_object), organization=request.org
            )
        else:
            self.form = self.form_class(
                initial=self.get_initial_for_create(request, **kwargs),
                organization=request.org,
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


class ChapterForm(ModelFormWithOrg):
    default_renderer = BootstrapFormRenderer

    class Meta:
        model = Chapter
        fields = ["num", "title"]
        labels = {
            "num": "Number",
        }
        widgets = {"num": TextInput(), "title": TextInput()}

    def clean(self) -> dict[str, Any]:
        result = super().clean()
        check_for_children(self.instance, "chapter", "sections")
        return result


class CreateUpdateChapter(CreateUpdateView):
    form_class = ChapterForm
    template_name = "edit_chapter.html"
    object_name = "chapter"
    role = UserRole.EDIT_MANUAL

    def get_success_url(self, instance):
        assert isinstance(instance, Chapter)
        if instance.deleted:
            return "/viewManual"
        return f"/viewChapter/{instance.id}"

    def can_edit(self, request: DstHttpRequest, instance: Model):
        assert isinstance(instance, Chapter)
        return instance.organization_id == request.org.id

    def pre_save(self, request: DstHttpRequest, instance: Model):
        assert isinstance(instance, Chapter)
        if instance.organization_id is None:
            instance.organization = request.org


def check_for_children(instance: Chapter | Section | None, name: str, child_name: str):
    if instance and instance.deleted and getattr(instance, child_name).exists():
        child_titles = [
            f'"{x}"'
            for x in getattr(instance, child_name).values_list("title", flat=True)
        ]
        child_titles_str = ", ".join(child_titles)
        raise ValidationError(
            f"This {name} cannot be deleted because it isn't empty. To delete it, first remove all {child_name} from within it. The current {child_name} are: {child_titles_str}."
        )


class SectionForm(ModelFormWithOrg):
    default_renderer = BootstrapFormRenderer

    def __init__(self, *args, organization=None, **kwargs) -> None:
        super().__init__(*args, organization=organization, **kwargs)
        self.fields["chapter"].queryset = Chapter.objects.filter(
            organization=organization
        ).order_by("num")

    class Meta:
        model = Section
        fields = ["chapter", "num", "title"]
        labels = {
            "num": "Number",
        }
        widgets = {"num": TextInput(), "title": TextInput()}

    def clean(self) -> dict[str, Any]:
        result = super().clean()
        check_for_children(self.instance, "section", "entries")
        return result


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


class EntryForm(ModelFormWithOrg):
    default_renderer = BootstrapFormRenderer

    effective_date = forms.DateField(initial=timezone.now().date())
    show_date_in_history = forms.BooleanField(
        required=False,
        initial=True,
        help_text="Uncheck this box to prevent the date from being be added to the list of dates shown beneath the rule. This could be useful when you are making a minor formatting change or you don't know the correct date to enter. A record of this change will always be preserved.",
    )

    def __init__(self, *args, organization=None, **kwargs) -> None:
        super().__init__(*args, organization=organization, **kwargs)
        self.fields["section"].queryset = Section.objects.filter(
            chapter__organization=organization
        ).order_by("chapter__num", "num")

    class Meta:
        model = Entry
        fields = ["section", "num", "title", "content"]
        labels = {
            "num": "Number",
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
        show_date_in_history=entry_form.cleaned_data["show_date_in_history"],
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
        if instance.deleted:
            return f"/viewChapter/{instance.section.chapter_id}#section_{instance.section_id}"
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
    chapters = chapters_with_entries(request.org)
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


def highlight_matches_markdown(value: str, pattern: re.Pattern) -> str:
    return pattern.sub(r"[[\1]]", value)


def highlight_matches_html(value: str, pattern: re.Pattern) -> str:
    # Unicode values that have no meaning
    open_tag = "\ue000"
    close_tag = "\ue001"

    value = pattern.sub(open_tag + r"\1" + close_tag, value)

    value = escape(value)
    value = value.replace(open_tag, '<span class="man-search-highlight">')
    value = value.replace(close_tag, "</span>")
    return mark_safe(value)


class SearchForm(Form):
    default_renderer = BootstrapFormRenderer

    search_string = forms.CharField(
        required=False,
        label="Search for",
        help_text="Chapter, section, and entry numbers and titles are searched. If a chapter or section matches, all rules in that chapter or section are displayed.",
    )
    include_deleted = forms.BooleanField(
        required=False, label="Include deleted/repealed entries?"
    )
    whole_words = forms.BooleanField(
        required=False,
        label="Match whole words only?",
        help_text="e.g. if you search for 'and', don't show items containing the word 'sand'",
    )


@login_required()
def search_manual(request: DstHttpRequest):
    form = SearchForm(request.GET)
    assert form.is_valid()
    search_string = form.cleaned_data["search_string"]
    search_words = [x for x in re.split(r"\s+", search_string) if x]

    entries = []

    if search_words:
        space = Value(" ")
        entry_query = (
            Entry.all_objects.filter(
                section__chapter__organization=request.org,
            )
            .annotate(
                full_content=Concat(
                    "section__chapter__num",
                    "section__num",
                    Value("."),
                    "num",
                    space,
                    "content",
                    space,
                    "title",
                    space,
                    "section__title",
                    space,
                    "section__chapter__title",
                    output_field=CharField(),
                )
            )
            .select_related("section__chapter")
            .prefetch_related(
                Prefetch(
                    "changes",
                    ManualChange.objects.order_by("date_entered").filter(
                        show_date_in_history=True
                    ),
                ),
            )
            .order_by("section__chapter__num", "section__num", "num")
        )
        if not form.cleaned_data["include_deleted"]:
            entry_query = entry_query.filter(
                deleted=False,
                section__deleted=False,
                section__chapter__deleted=False,
            )
        for word in search_words:
            if form.cleaned_data["whole_words"]:
                entry_query = entry_query.filter(full_content__iregex=rf"\y{word}\y")
            else:
                entry_query = entry_query.filter(full_content__icontains=word)
        entries = list(entry_query)

    words_sorted = sorted(search_words, key=len, reverse=True)
    elements = [re.escape(word) for word in words_sorted]
    if form.cleaned_data["whole_words"]:
        elements = [rf"\b{x}\b" for x in elements]
    pattern = re.compile("(" + "|".join(elements) + ")", re.IGNORECASE)

    for entry in entries:
        entry.number = highlight_matches_html(entry.number(), pattern)
        entry.section.number = highlight_matches_html(entry.section.number(), pattern)
        entry.section.chapter.num = highlight_matches_html(
            entry.section.chapter.num, pattern
        )
        entry.title = highlight_matches_html(entry.title, pattern)
        entry.section.title = highlight_matches_html(entry.section.title, pattern)
        entry.section.chapter.title = highlight_matches_html(
            entry.section.chapter.title, pattern
        )
        entry.content = highlight_matches_markdown(entry.content, pattern)

    org_config = get_org_config(request.org)

    return render_main_template(
        request,
        render_to_string(
            "manual_search.html",
            {
                "search_string": " ".join(search_words),
                "entries": entries,
                "org_config": org_config,
                "can_edit": request.user.hasRole(UserRole.EDIT_MANUAL),
                "form": form,
            },
        ),
        f"{org_config.str_manual_title} Search: {search_string}",
        "search",
    )


@login_required()
def preview_entry(request: DstHttpRequest, object_id: int | None = None):
    existing = None
    if object_id:
        existing = Entry.all_objects.filter(id=object_id).first()
        # When previewing edits to a deleted entry, show it as undeleted
        existing.deleted = False

    entry_form = EntryForm(
        request.POST, instance=deepcopy(existing), organization=request.org
    )
    temp_entry: Entry = entry_form.save(commit=False)
    changes = temp_entry.changes_for_render()

    if new_change := get_manual_change_for_entry(
        request.user, entry_form, existing, temp_entry
    ):
        if new_change.show_date_in_history:
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


def get_chapter_response(chapter: Chapter, request: DstHttpRequest) -> HttpResponse:
    return render_main_template(
        request,
        render_to_string(
            "view_chapter.html",
            {
                "chapter": chapter,
                "org_config": get_org_config(request.org),
                "can_edit": request.user.hasRole(UserRole.EDIT_MANUAL),
            },
        ),
        f"{chapter.num} {chapter.title}",
    )


@login_required()
def print_manual_chapter(request: DstHttpRequest, chapter_id: int):
    org = request.org
    org_config = get_org_config(org)

    if chapter_id == -1:
        # Render complete manual (TOC + all chapters)
        chapters = chapters_with_entries(org)
        html = get_html_from_response(
            render_main_template(
                request,
                render_to_string(
                    "view_manual_toc.html",
                    {
                        "chapters": chapters,
                        "org_config": org_config,
                        "include_content": True,
                    },
                ),
                "",
            )
        )

        return create_pdf_response(
            render_html_to_pdf(html), f"{org.short_name}_Manual.pdf"
        )

    elif chapter_id == -2:
        # Render TOC only
        toc_html = get_html_from_response(
            render_main_template(
                request,
                render_to_string(
                    "view_manual_toc.html",
                    {
                        "chapters": chapters_with_entries(org),
                        "org_config": org_config,
                    },
                ),
                "",
            )
        )

        return create_pdf_response(
            render_html_to_pdf(toc_html), f"{org.short_name}_Manual_TOC.pdf"
        )

    else:
        # Render specific chapter
        chapter = chapters_with_entries(org).filter(id=chapter_id).first()
        if chapter is None:
            return HttpResponseNotFound()

        pdf_bytes = render_html_to_pdf(
            get_html_from_response(get_chapter_response(chapter, request))
        )
        return create_pdf_response(
            pdf_bytes, f"{org.short_name}_Chapter_{chapter.num}.pdf"
        )
