import re
from datetime import date, datetime

import mistletoe
from django import template
from django.utils import timezone
from django.utils.safestring import mark_safe
from mistletoe.html_renderer import HtmlRenderer
from mistletoe.span_token import SpanToken

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


class SearchHighlightToken(SpanToken):
    pattern = re.compile(r"\[\[([^]]*)\]\]")


class DstRenderer(HtmlRenderer):
    def __init__(self):
        super().__init__(SearchHighlightToken, process_html_tokens=False)

    def render_search_highlight_token(self, token):
        return f'<span class="man-search-highlight">{self.render_inner(token)}</span>'


@register.filter
def markdown(text):
    if text is None:
        return ""
    assert isinstance(text, str)
    # Replace bullets at the beginning of lines with asterisks so that if
    # you copy paste content
    # from a PDF (or something?) and it has a real bullet character in it,
    # we will render it as a list properly. TCS has some rules like this.
    text = re.sub(r"^(\s*)• ", "\\1* ", text, flags=re.MULTILINE)

    with DstRenderer() as renderer:
        return mark_safe(renderer.render(mistletoe.Document(text)))


@register.filter(name="list")
def my_list(obj):
    return list(obj)


@register.filter
def lookup(dictionary, key):
    """Template filter to look up a value in a dictionary"""
    if hasattr(dictionary, "get"):
        return dictionary.get(key)
    elif hasattr(dictionary, "__getitem__"):
        try:
            return dictionary[key]
        except (KeyError, IndexError, TypeError):
            return None
    return None


@register.filter
def format_number(value):
    """Format a number to 1 decimal place"""
    try:
        return f"{float(value):.1f}"
    except (ValueError, TypeError):
        return "0.0"


@register.filter
def format_as_percent(value):
    """Format a value as a percentage"""
    try:
        return f"{float(value) * 100:.1f}%"
    except (ValueError, TypeError):
        return "0.0%"


@register.filter
def format_date_for_input(d):
    """Format date for HTML date input"""
    if d is None:
        return ""
    if isinstance(d, datetime):
        d = d.date()
    return d.strftime("%Y-%m-%d")
