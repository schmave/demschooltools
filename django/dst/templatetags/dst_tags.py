import json
import re
from datetime import date, datetime

import mistletoe
from django import template
from django.conf import settings
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
def rollbarSnippet(context, d: datetime | date | None = None):
    request = context["request"]

    if not settings.ROLLBAR_FRONTEND_TOKEN:
        return mark_safe("<!-- No Rollbar because ROLLBAR_FRONTEND_TOKEN is empty -->")

    user = request.user
    rollbarConfig = {
        "accessToken": settings.ROLLBAR_FRONTEND_TOKEN,
        "captureUncaught": True,
        "captureUnhandledRejections": True,
        "payload": {
            "environment": settings.ROLLBAR_ENVIRONMENT,
            "person": {
                "id": user.id if user.is_authenticated else None,
                "email": user.email if user.is_authenticated else None,
            },
        },
    }

    return mark_safe(
        """
        <script>
            var _rollbarConfig = %s;
            // Rollbar Snippet
            !function(){"use strict";function r(r,o,n){if(o.hasOwnProperty&&o.hasOwnProperty("addEventListener")){for(var e=o.addEventListener;e._rollbarOldAdd&&e.belongsToShim;)e=e._rollbarOldAdd;var t=function(o,n,t){e.call(this,o,r.wrap(n),t)};t._rollbarOldAdd=e,t.belongsToShim=n,o.addEventListener=t;for(var a=o.removeEventListener;a._rollbarOldRemove&&a.belongsToShim;)a=a._rollbarOldRemove;var l=function(r,o,n){a.call(this,r,o&&o._rollbar_wrapped||o,n)};l._rollbarOldRemove=a,l.belongsToShim=n,o.removeEventListener=l}}function o(r,n){this.impl=r(n,this),this.options=n,function(r){for(var o=function(r){return function(){var o=Array.prototype.slice.call(arguments,0);if(this.impl[r])return this.impl[r].apply(this.impl,o)}},n="log,debug,info,warn,warning,error,critical,global,configure,handleUncaughtException,handleAnonymousErrors,handleUnhandledRejection,_createItem,wrap,loadFull,shimId,captureEvent,captureDomContentLoaded,captureLoad".split(","),e=0;e<n.length;e++)r[n[e]]=o(n[e])}(o.prototype)}o.prototype._swapAndProcessMessages=function(r,o){var n,e,t;for(this.impl=r(this.options);n=o.shift();)e=n.method,t=n.args,this[e]&&"function"==typeof this[e]&&("captureDomContentLoaded"===e||"captureLoad"===e?this[e].apply(this,[t[0],n.ts]):this[e].apply(this,t));return this};var n=o;function e(r){return e="function"==typeof Symbol&&"symbol"==typeof Symbol.iterator?function(r){return typeof r}:function(r){return r&&"function"==typeof Symbol&&r.constructor===Symbol&&r!==Symbol.prototype?"symbol":typeof r},e(r)}function t(r){return function(){try{return r.apply(this,arguments)}catch(r){try{console.error("[Rollbar]: Internal error",r)}catch(r){}}}}var a=0;function l(r,o){this.options=r,this._rollbarOldOnError=null;var n=a++;this.shimId=function(){return n},"undefined"!=typeof window&&window._rollbarShims&&(window._rollbarShims[n]={handler:o,messages:[]})}var i=function(r,o){return new l(r,o)},s=function(r){return new n(i,r)};function d(r){return t((function(){var o=Array.prototype.slice.call(arguments,0),n={shim:this,method:r,args:o,ts:new Date};window._rollbarShims[this.shimId()].messages.push(n)}))}l.prototype.loadFull=function(r,o,n,e,a){var l=!1,i=o.createElement("script"),s=o.getElementsByTagName("script")[0],d=s.parentNode;i.crossOrigin="",i.src=e.rollbarJsUrl,n||(i.async=!0),i.onload=i.onreadystatechange=t((function(){if(!(l||this.readyState&&"loaded"!==this.readyState&&"complete"!==this.readyState)){i.onload=i.onreadystatechange=null;try{d.removeChild(i)}catch(r){}l=!0,function(){var o;if(void 0===r._rollbarDidLoad){o=new Error("rollbar.js did not load");for(var n,e,t,l,i=0;n=r._rollbarShims[i++];)for(n=n.messages||[];e=n.shift();)for(t=e.args||[],i=0;i<t.length;++i)if("function"==typeof(l=t[i])){l(o);break}}"function"==typeof a&&a(o)}()}})),d.insertBefore(i,s)},l.prototype.wrap=function(r,o,n){try{var e;if(e="function"==typeof o?o:function(){return o||{}},"function"!=typeof r)return r;if(r._isWrap)return r;if(!r._rollbar_wrapped&&(r._rollbar_wrapped=function(){n&&"function"==typeof n&&n.apply(this,arguments);try{return r.apply(this,arguments)}catch(n){var o=n;throw o&&("string"==typeof o&&(o=new String(o)),o._rollbarContext=e()||{},o._rollbarContext._wrappedSource=r.toString(),window._rollbarWrappedError=o),o}},r._rollbar_wrapped._isWrap=!0,r.hasOwnProperty))for(var t in r)r.hasOwnProperty(t)&&(r._rollbar_wrapped[t]=r[t]);return r._rollbar_wrapped}catch(o){return r}};for(var c="log,debug,info,warn,warning,error,critical,global,configure,handleUncaughtException,handleAnonymousErrors,handleUnhandledRejection,captureEvent,captureDomContentLoaded,captureLoad".split(","),p=0;p<c.length;++p)l.prototype[c[p]]=d(c[p]);var u="https://cdn.rollbar.com/rollbarjs/refs/tags/v3.0.0-beta.5/rollbar.min.js";if(_rollbarConfig=_rollbarConfig||{},!_rollbarConfig.rollbarJsUrl){var f="replay"in _rollbarConfig;_rollbarConfig.rollbarJsUrl=f?u.replace("rollbar.min.js","rollbar.replay.min.js"):u}_rollbarConfig.async=void 0===_rollbarConfig.async||_rollbarConfig.async;var b,h=function(o,n){if(o){var a=n.globalAlias||"Rollbar";if("object"===e(o[a]))return o[a];o._rollbarShims={},o._rollbarWrappedError=null;var l=new s(n);return t((function(){n.captureUncaught&&(l._rollbarOldOnError=o.onerror,function(r,o){if(r){var n;if("function"==typeof o._rollbarOldOnError)n=o._rollbarOldOnError;else if(r.onerror){for(n=r.onerror;n._rollbarOldOnError;)n=n._rollbarOldOnError;o._rollbarOldOnError=n}o.handleAnonymousErrors();var e=function(){var e=Array.prototype.slice.call(arguments,0);!function(r,o,n,e){r._rollbarWrappedError&&(e[4]||(e[4]=r._rollbarWrappedError),e[5]||(e[5]=r._rollbarWrappedError._rollbarContext),r._rollbarWrappedError=null);var t=o.handleUncaughtException.apply(o,e);n&&n.apply(r,e),"anonymous"===t&&(o.anonymousErrorsPending+=1)}(r,o,n,e)};e._rollbarOldOnError=n,r.onerror=e}}(o,l),n.wrapGlobalEventHandlers&&function(o,n,e){if(o){var t,a,l="EventTarget,Window,Node,ApplicationCache,AudioTrackList,ChannelMergerNode,CryptoOperation,EventSource,FileReader,HTMLUnknownElement,IDBDatabase,IDBRequest,IDBTransaction,KeyOperation,MediaController,MessagePort,ModalWindow,Notification,SVGElementInstance,Screen,TextTrack,TextTrackCue,TextTrackList,WebSocket,WebSocketWorker,Worker,XMLHttpRequest,XMLHttpRequestEventTarget,XMLHttpRequestUpload".split(",");for(t=0;t<l.length;++t)o[a=l[t]]&&o[a].prototype&&r(n,o[a].prototype,e)}}(o,l,!0)),n.captureUnhandledRejections&&function(r,o){if(r){"function"==typeof r._rollbarURH&&r._rollbarURH.belongsToShim&&r.removeEventListener("unhandledrejection",r._rollbarURH);var n=function(r){var n,e,t;try{n=r.reason}catch(r){n=void 0}try{e=r.promise}catch(r){e="[unhandledrejection] error getting `promise` from event"}try{t=r.detail,!n&&t&&(n=t.reason,e=t.promise)}catch(r){}n||(n="[unhandledrejection] error getting `reason` from event"),o&&o.handleUnhandledRejection&&o.handleUnhandledRejection(n,e)};n.belongsToShim=!0,r._rollbarURH=n,r.addEventListener("unhandledrejection",n)}}(o,l);var t=n.autoInstrument;return!1!==n.enabled&&(void 0===t||!0===t||function(r){return!("object"!==e(r)||void 0!==r.page&&!r.page)}(t))&&o.addEventListener&&(o.addEventListener("load",l.captureLoad.bind(l)),o.addEventListener("DOMContentLoaded",l.captureDomContentLoaded.bind(l))),o[a]=l,l}))()}}(window,_rollbarConfig),y=(b=_rollbarConfig,function(r){if(!r&&!window._rollbarInitialized){for(var o,n,e=(b=b||{}).globalAlias||"Rollbar",t=window.rollbar,a=function(r){return new t(r)},l=0;o=window._rollbarShims[l++];)n||(n=o.handler),o.handler._swapAndProcessMessages(a,o.messages);window[e]=n,window._rollbarInitialized=!0}});window.rollbar=s,h.loadFull(window,document,!_rollbarConfig.async,_rollbarConfig,y)}();
            // End Rollbar Snippet
        </script>
"""
        % (json.dumps(rollbarConfig))
    )


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


@register.filter
def format_time(t):
    """Format time object as h:mm AM/PM"""
    if t is None:
        return "---"
    # Convert time to datetime for strftime formatting
    from datetime import datetime

    dt = datetime.combine(datetime.today(), t)
    return dt.strftime("%-I:%M %p")


@register.filter
def add_days(d, days):
    """Add days to a date"""
    from datetime import timedelta

    if d is None:
        return None
    if isinstance(d, datetime):
        d = d.date()
    return d + timedelta(days=days)
