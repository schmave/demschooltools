from datetime import date, datetime

import mistletoe
from django import template
from django.utils import timezone
from django.utils.safestring import mark_safe

from dst.org_config import OrgConfig

register = template.Library()


@register.simple_tag(takes_context=True)
def yymmddDate(context, d: datetime | date | None = None):
    org_config: OrgConfig = context["org_config"]

    if d is None or isinstance(d, datetime):
        d = timezone.localtime(d)

    if org_config.euro_dates:
        return d.strftime("%d-%m-%Y")

    return d.strftime("%Y-%m-%d")


@register.simple_tag(takes_context=True)
def dateAndTime(context, d: datetime | date | None = None):
    org_config: OrgConfig = context["org_config"]

    if d is None or isinstance(d, datetime):
        d = timezone.localtime(d)

    if org_config.euro_dates:
        return d.strftime("%d-%m-%Y %H:%M")

    return d.strftime("%Y-%m-%d %-I:%M%p")


@register.filter
def markdown(text):
    if text is None:
        return ""
    assert isinstance(text, str)
    return mark_safe(mistletoe.markdown(text))


@register.filter(name="list")
def my_list(obj):
    return list(obj)
