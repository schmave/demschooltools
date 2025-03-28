import zoneinfo

from django.utils import timezone


class TimezoneMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if hasattr(request, "org"):
            timezone.activate(zoneinfo.ZoneInfo(request.org.timezone))
        else:
            timezone.activate(zoneinfo.ZoneInfo("America/New_York"))

        return self.get_response(request)
