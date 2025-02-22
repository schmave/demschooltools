import zoneinfo

from django.utils import timezone


class TimezoneMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if hasattr(request, "school"):
            timezone.activate(zoneinfo.ZoneInfo(request.school.timezone))
        else:
            timezone.deactivate()

        return self.get_response(request)
