import random
import re

from django.contrib.staticfiles.storage import (
    ManifestStaticFilesStorage,
    StaticFilesStorage,
)


class RandomVersionStaticFilesStorage(StaticFilesStorage):
    """
    For some reason Django's static files system doesn't prevent
    the browser from caching static content in local development. This works around that.
    """

    def url(self, name):
        base_url = super().url(name)
        return f"{base_url}?v={random.randint(0, 999999)}"


VITE_HASH_REGEX = re.compile(r"^.+[\.-][0-9a-zA-Z_-]{8,12}\..+$")


def _is_excluded(name):
    # django-vite gets confused if django staticfiles hashes
    # files on top of vite already hashing them.
    # https://github.com/MrBin99/django-vite/issues/86
    return VITE_HASH_REGEX.match(name) is not None


class SelectiveManifestStaticFilesStorage(ManifestStaticFilesStorage):
    def hashed_name(self, name, content=None, filename=None):
        if _is_excluded(name):
            return name
        else:
            return super().hashed_name(name, content, filename)
