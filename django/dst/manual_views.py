from datetime import datetime
from typing import Any, Type

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
from dst.models import Chapter, Entry, ManualChange, Section, User, UserRole
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
        },
    )


@login_required()
def view_manual(request: HttpRequest):
    chapters = chapter_with_entries().filter(organization=request.org, deleted=False)

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


def chapter_with_entries() -> QuerySet[Chapter]:
    return Chapter.objects.prefetch_related(
        Prefetch(
            "sections",
            Section.objects.order_by("num")
            .filter(deleted=False)
            .prefetch_related(
                Prefetch(
                    "entries",
                    Entry.objects.filter(deleted=False)
                    .order_by("num")
                    .prefetch_related(
                        Prefetch(
                            "changes", ManualChange.objects.order_by("date_entered")
                        )
                    ),
                )
            ),
        )
    ).order_by("num")


@login_required()
def view_chapter(request: HttpRequest, chapter_id: int):
    chapter = (
        chapter_with_entries().filter(id=chapter_id, organization=request.org).first()
    )

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


class ChapterForm(ModelForm):
    default_renderer = BootstrapFormRenderer()

    class Meta:
        model = Chapter
        fields = ["id", "num", "title", "deleted"]
        labels = {
            "num": "Number",
            "deleted": "Check this to delete",
        }
        widgets = {"num": TextInput(), "title": TextInput()}


def on_manual_change(*args, **kwargs):
    with connection.cursor() as cursor:
        cursor.execute("REFRESH MATERIALIZED VIEW entry_index WITH DATA")


post_save.connect(on_manual_change, sender=Chapter)
post_save.connect(on_manual_change, sender=Section)
post_save.connect(on_manual_change, sender=Entry)


class EditUpdateView(View):
    form: Type[ModelForm] = ModelForm
    model: Type[Model] = Model
    template_name = ""
    object_name = ""
    view_url_base = ""
    role = ""  # value from UserRole, e.g. UserRole.EDIT_MANUAL

    def render(self, request: HttpRequest, form: ModelForm):
        return render_main_template(
            request,
            render_to_string(
                self.template_name,
                {
                    "org_config": get_org_config(request.org),
                    "form": form,
                    "is_new": self.existing_object is None,
                },
                request=request,
            ),
            f"Edit a {self.object_name}"
            if self.existing_object
            else f"Add new {self.object_name}",
        )

    def load_object(self, request: HttpRequest, object_id: int | None):
        self.existing_object = self.model.objects.filter(
            id=object_id, organization=request.org
        ).first()

    def post(self, request: HttpRequest, object_id: int | None = None):
        if request.method == "POST":
            form = self.form(request.POST, instance=self.existing_object)
            if form.is_valid():
                new_object = form.save(commit=False)
                if new_object.organization_id is None:
                    new_object.organization = request.org
                new_object.save()

                return redirect(f"{self.view_url_base}/{new_object.id}")
        else:
            form = self.form(instance=self.existing_object)

    def get(self, request: HttpRequest, object_id: int | None = None):
        return self.render(request, self.form(instance=self.existing_object))

    def dispatch(
        self,
        request: HttpRequest,
        *args: Any,
        object_id: int | None = None,
        **kwargs: Any,
    ) -> HttpResponse:
        if not request.user.is_authenticated:
            raise PermissionError()
        assert isinstance(request.user, User)
        if not request.user.hasRole(self.role):
            raise PermissionError()
        self.load_object(request, object_id)
        return super().dispatch(request, *args, **kwargs)


class EditUpdateChapter(EditUpdateView):
    form = ChapterForm
    model = Chapter
    template_name = "edit_chapter.html"
    object_name = "chapter"
    view_url_base = "/viewChapter"
    role = UserRole.EDIT_MANUAL
