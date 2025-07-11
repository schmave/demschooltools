from copy import deepcopy
from datetime import datetime, timedelta
from typing import Any, Type

from django.conf import settings
from django.contrib.auth.decorators import login_required
from django.db import connection
from django.db.models import Model, Prefetch, QuerySet
from django.db.models.signals import post_save
from django.forms import ModelForm, TextInput
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
from dst.org_config import get_org_config

# TODO: login_required in this file should redirect
# to the Play framework login page, not the Custodia one.


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


@login_required()
def view_manual_changes(request: DstHttpRequest):
    begin_date = None
    try:
        begin_date = datetime.strptime(request.GET["begin_date"], "%Y-%m-%d")
    except:
        begin_date = timezone.now().replace(
            hour=0, minute=0, second=0, microsecond=0
        ) - timedelta(days=7)

    changes = list(
        ManualChange.objects.filter(
            date_entered__gt=begin_date,
            entry__section__chapter__organization=request.org,
        )
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
        form = self.form_class(request.POST, instance=deepcopy(self.existing_object))
        if form.is_valid():
            new_object = form.save(commit=False)
            self.pre_save(request, new_object)
            new_object.save()
            self.post_save(request, new_object)

            return redirect(self.get_success_url(new_object))

        return self.render(request, form)

    def get(self, request: DstHttpRequest, **kwargs):
        if self.existing_object:
            form = self.form_class(instance=deepcopy(self.existing_object))
        else:
            form = self.form_class(
                initial=self.get_initial_for_create(request, **kwargs)
            )
        return self.render(request, form)

    def can_edit(self, request: DstHttpRequest, instance: Model) -> bool:
        return True

    def dispatch(
        self,
        request: DstHttpRequest,
        object_id: int | None = None,
        **kwargs: Any,
    ) -> HttpResponse:
        if not request.user.is_authenticated:
            raise PermissionError()
        assert isinstance(request.user, User)
        if not request.user.hasRole(self.role):
            raise PermissionError()

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

    class Meta:
        model = Entry
        fields = ["section", "num", "title", "deleted", "content"]
        labels = {
            "num": "Number",
            "deleted": "Check this to delete",
        }
        widgets = {"num": TextInput(), "title": TextInput()}


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

        common_data = dict(
            entry=instance,
            new_content=instance.content,
            new_num=instance.number(),
            new_title=instance.title,
        )

        if self.existing_object is None:
            ManualChange.objects.create(was_created=True, **common_data)
        else:
            common_data.update(
                old_content=self.existing_object.content,
                old_num=self.existing_object.number(),
                old_title=self.existing_object.title,
            )
            if instance.deleted and not self.existing_object.deleted:
                ManualChange.objects.create(was_deleted=True, **common_data)
            else:
                if (
                    common_data["old_content"] != common_data["new_content"]
                    or common_data["old_title"] != common_data["new_title"]
                    or common_data["old_num"] != common_data["new_num"]
                ):
                    ManualChange.objects.create(**common_data)


@login_required
def view_entry(request: DstHttpRequest):
    temp_entry = EntryForm(request.POST).save(commit=False)
    return render(
        request,
        "view_entry.html",
        {
            "entry": temp_entry,
            "org_config": get_org_config(request.org),
        },
    )
