from datetime import datetime

from django.http import HttpRequest
from django.shortcuts import render
from django.template.loader import render_to_string
from django.utils.safestring import mark_safe

from dst.models import Chapter
from dst.org_config import get_org_config


def render_main_template(request: HttpRequest, content: str):
    return render(
        request,
        "main.html",
        {
            "title": f"{request.org.short_name} Manual",
            "menu": "manual",
            "selectedBtn": "toc",
            "content": mark_safe(content),
            "current_username": request.user.email
            if request.user.is_authenticated
            else None,
            "is_user_logged_in": request.user.is_authenticated,
            "org_config": get_org_config(request.org),
        },
    )


def view_manual(request: HttpRequest):
    # Get chapters and other data
    chapters = Chapter.objects.filter(organization=request.org).prefetch_related(
        "sections__entries"
    )

    # Render the manual template to a string
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
    )
