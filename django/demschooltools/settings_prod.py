import os

from django.conf.global_settings import SECURE_SSL_REDIRECT

from demschooltools.settings import *  # noqa: F403

CUSTODIA_JS_LINK = ""
SECRET_KEY = os.environ["DJANGO_SECRET_KEY"]

DEBUG = False

ALLOWED_HOSTS = ["*.demschooltools.com"]

# Profiling
assert "silk" not in INSTALLED_APPS

CSRF_COOKIE_SECURE = True
SESSION_COOKIE_SECURE = True
SECURE_SSL_REDIRECT = True

AUTHENTICATION_BACKENDS = ["demschooltools.auth.PlaySessionBackend"]

JWT_KEY = os.environ["APPLICATION_SECRET"]

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "OPTIONS": {"options": "-c search_path=public,overseer"},
        "NAME": "school_crm_from_prod",
        "PORT": "5432",
        "HOST": "localhost",
        "USER": "postgres",
        "PASSWORD": os.environ["DB_PASSWORD"],
        "CONN_MAX_AGE": 60,
    },
}

STATIC_ROOT = "/www/django-static/"
