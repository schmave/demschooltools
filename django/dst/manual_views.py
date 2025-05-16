from datetime import datetime

from django.forms import ModelForm, TextInput
from django.http import HttpRequest
from django.shortcuts import redirect, render
from django.template.loader import render_to_string
from django.utils.safestring import mark_safe

from dst.models import Chapter
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
    chapters = Chapter.objects.filter(organization=request.org).prefetch_related(
        "sections__entries"
    )

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


def view_chapter(request: HttpRequest, chapter_id: int):
    chapter = (
        Chapter.objects.filter(id=chapter_id, organization=request.org)
        .prefetch_related("sections__entries")
        .first()
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
    class Meta:
        model = Chapter
        fields = ["id", "num", "title", "deleted"]
        labels = {
            "num": "Number",
            "deleted": "Check this to delete",
        }
        widgets = {"num": TextInput(), "title": TextInput()}


def edit_chapter(request: HttpRequest, chapter_id: int | None = None):
    if request.method == "POST":
        form = ChapterForm(request.POST)
        if form.is_valid():
            chapter = form.save()
            # TODO: Call onManualChange();
            return redirect(f"/viewChapter/{chapter.id}")
    else:
        chapter = Chapter.objects.filter(
            id=chapter_id, organization=request.org
        ).first()
        form = ChapterForm(instance=chapter)

    return render_main_template(
        request,
        render_to_string(
            "edit_chapter.html",
            {
                "org_config": get_org_config(request.org),
                "form": form,
            },
            request=request,
        ),
        "Edit a chapter",
    )
