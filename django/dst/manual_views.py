from datetime import datetime

from django.db.models import Prefetch, QuerySet
from django.forms import ModelForm, TextInput
from django.http import HttpRequest
from django.shortcuts import redirect, render
from django.template.loader import render_to_string
from django.utils.safestring import mark_safe

from demschooltools.form_renderer import BootstrapFormRenderer
from dst.models import Chapter, Entry, Section
from dst.org_config import get_org_config


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
                Prefetch("entries", Entry.objects.filter(deleted=False).order_by("num"))
            ),
        )
    ).order_by("num")


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


def edit_chapter(request: HttpRequest, chapter_id: int | None = None):
    existing_chapter = Chapter.objects.filter(
        id=chapter_id, organization=request.org
    ).first()

    if request.method == "POST":
        form = ChapterForm(request.POST, instance=existing_chapter)
        if form.is_valid():
            chapter = form.save(commit=False)
            if chapter.organization_id is None:
                chapter.organization = request.org
            chapter.save()
            # TODO: Call onManualChange();
            return redirect(f"/viewChapter/{chapter.id}")
    else:
        form = ChapterForm(instance=existing_chapter)

    return render_main_template(
        request,
        render_to_string(
            "edit_chapter.html",
            {
                "org_config": get_org_config(request.org),
                "form": form,
                "is_new": existing_chapter is None,
            },
            request=request,
        ),
        "Edit a chapter",
    )
