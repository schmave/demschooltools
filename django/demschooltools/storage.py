import random

from django.contrib.staticfiles.storage import StaticFilesStorage


class RandomVersionStaticFilesStorage(StaticFilesStorage):
    """
    For some reason Django's static files system doesn't prevent
    the browser from caching static content in local development. This works around that.
    """

    def url(self, name):
        base_url = super().url(name)
        return f"{base_url}?v={random.randint(0, 999999)}"
