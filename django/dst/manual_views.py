from datetime import datetime
from typing import Any, Type

from django.conf import settings
from django.contrib.auth.decorators import login_required
from django.db import connection
from django.db.models import Model, Prefetch, QuerySet
from django.db.models.signals import post_save
from django.forms import ModelForm, TextInput
from django.http import HttpRequest
from django.http.response import HttpResponse as HttpResponse
from django.shortcuts import redirect, render
from django.template.loader import render_to_string
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


def render_main_template(
    request: HttpRequest,
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
def view_manual(request: HttpRequest):
    chapters = chapter_with_entries(request.org)

    return render_main_template(
        request,
        render_to_string(
            "view_manual.html",
            {
                "chapters": chapters,
                "current_date": datetime.now().strftime("%Y-%m-%d"),
                "org_config": get_org_config(request.org),
            },
        ),
        f"{request.org.short_name} Manual",
        selected_button="toc",
    )


def chapter_with_entries(org: Organization) -> QuerySet[Chapter]:
    return Chapter.objects.filter(organization=org).prefetch_related(
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
def view_chapter(request: HttpRequest, chapter_id: int):
    chapter = chapter_with_entries(request.org).filter(id=chapter_id).first()

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

    def set_defaults_on_create(self, request: HttpRequest, instance: Model):
        pass

    def get_initial_for_create(self, request: HttpRequest, **kwargs):
        return {}

    def get_form_context(
        self, request: HttpRequest, form: ModelForm, context: dict
    ) -> dict:
        return context

    def render(self, request: HttpRequest, form: ModelForm):
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

    def load_object(self, request: HttpRequest, object_id: int):
        assert self.form_class._meta.model is not None, (
            "Your form must include a model in class Meta"
        )
        self.existing_object = self.form_class._meta.model.objects.get(id=object_id)

    def post(self, request: HttpRequest, **kwargs):
        form = self.form_class(request.POST, instance=self.existing_object)
        if form.is_valid():
            new_object = form.save(commit=False)
            if self.existing_object is None:
                self.set_defaults_on_create(request, new_object)
            new_object.save()

            return redirect(self.get_success_url(new_object))

        return self.render(request, form)

    def get(self, request: HttpRequest, **kwargs):
        if self.existing_object:
            form = self.form_class(instance=self.existing_object)
        else:
            form = self.form_class(
                initial=self.get_initial_for_create(request, **kwargs)
            )
        return self.render(request, form)

    def can_edit(self, request: HttpRequest, instance: Model):
        return True

    def dispatch(
        self,
        request: HttpRequest,
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

            if not self.can_edit(request, self.existing_object):
                raise PermissionError()

        return super().dispatch(request, object_id=object_id, **kwargs)


class ChapterForm(ModelForm):
    default_renderer = BootstrapFormRenderer()

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

    def can_edit(self, request: HttpRequest, instance: Model):
        assert isinstance(instance, Chapter)
        return instance.organization_id == request.org.id

    def set_defaults_on_create(self, request: HttpRequest, instance: Model):
        assert isinstance(instance, Chapter)
        if instance.organization_id is None:
            instance.organization = request.org


class SectionForm(ModelForm):
    default_renderer = BootstrapFormRenderer()

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

    def can_edit(self, request: HttpRequest, instance: Model):
        assert isinstance(instance, Section)
        return instance.chapter.organization_id == request.org.id

    def get_initial_for_create(self, request: HttpRequest, **kwargs):
        self.chapter = Chapter.objects.get(
            organization=request.org, id=kwargs.get("chapter_id")
        )
        return {"chapter": self.chapter}

    def get_form_context(
        self, request: HttpRequest, form: ModelForm, context: dict
    ) -> dict:
        chapter = self.chapter or form.instance.chapter
        context["chapter_num"] = chapter.num
        return context


class EntryForm(ModelForm):
    default_renderer = BootstrapFormRenderer()

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

    def can_edit(self, request: HttpRequest, instance: Model):
        assert isinstance(instance, Entry)
        return instance.section.chapter.organization_id == request.org.id

    def get_initial_for_create(self, request: HttpRequest, **kwargs):
        self.section = Section.objects.get(
            chapter__organization=request.org, id=kwargs.get("section_id")
        )
        return {"section": self.section}

    def get_form_context(
        self, request: HttpRequest, form: ModelForm, context: dict
    ) -> dict:
        section = self.section or form.instance.section
        context["section_number"] = section.number()
        return context


@login_required
def view_entry(request: HttpRequest):
    temp_entry = EntryForm(request.POST).save(commit=False)
    print(vars(temp_entry))
    return render(
        request,
        "view_entry.html",
        {
            "entry": temp_entry,
            "org_config": get_org_config(request.org),
        },
    )
