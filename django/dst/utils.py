from django.conf import settings
from django.http import HttpRequest
from django.shortcuts import render
from django.utils.safestring import mark_safe

from dst.models import Organization
from dst.org_config import get_org_config


class DstHttpRequest(HttpRequest):
    org: Organization


def render_main_template(
    request: DstHttpRequest,
    menu: str,
    content: str,
    title: str,
    selected_button: str | None = None,
):
    return render(
        request,
        "main.html",
        {
            "title": title,
            "menu": menu,
            "selectedBtn": selected_button,
            "content": mark_safe(content),
            "current_username": request.user.email
            if request.user.is_authenticated
            else None,
            "is_user_logged_in": request.user.is_authenticated,
            "org_config": get_org_config(request.org),
            "rollbar_environment": settings.ROLLBAR_ENVIRONMENT,
            "rollbar_token": settings.ROLLBAR_FRONTEND_TOKEN,
        },
    )
