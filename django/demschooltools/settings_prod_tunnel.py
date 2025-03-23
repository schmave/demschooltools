import os

from demschooltools.settings import *  # noqa: F403

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": "school_crm",
        "PORT": "5433",
        "HOST": "localhost",
        "USER": "evan",
        "PASSWORD": os.environ["DB_PASSWORD"],
        "CONN_MAX_AGE": 60,
    },
}
