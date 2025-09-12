from datetime import date, datetime, time

from django import forms
from django.conf import settings
from django.contrib.auth.decorators import login_required as django_login_required
from django.http import HttpRequest
from django.http.response import HttpResponse as HttpResponse
from django.shortcuts import render
from django.utils import timezone
from django.utils.safestring import mark_safe

from dst.models import Organization
from dst.org_config import get_org_config


class DateToDatetimeField(forms.DateField):
    """
    Like a DateField, except that the cleaned value is a timezone-aware
    datetime object at midnight on the selected date.
    """

    def to_python(self, value):
        date_value = super().to_python(value)
        if date_value is None:
            return None

        assert isinstance(date_value, date)
        return timezone.make_aware(datetime.combine(date_value, time()))


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


def login_required():
    return django_login_required(login_url="/login")


def get_html_from_response(response: HttpResponse) -> str:
    return response.content.decode("utf-8")
