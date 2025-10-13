"""
For more information on this file, see
https://docs.djangoproject.com/en/5.1/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/5.1/ref/settings/
"""

import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()

# Build paths inside the project like this: BASE_DIR / 'subdir'.
BASE_DIR = Path(__file__).resolve().parent.parent


# Quick-start development settings - unsuitable for production
# See https://docs.djangoproject.com/en/3.2/howto/deployment/checklist/

CUSTODIA_JS_LINK = '<script src="/django-static/js/custodia.js"></script>'

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = "django-insecure-8*$0&(xq!3-)o0p%f@kusqqi1^02knn!3c8t)+n&z*cs_89cf*"

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = True

ALLOWED_HOSTS = ["localhost"]

# Profiling
SILK_ENABLED = False
PYINSTRUMENT_PROFILE_DIR = "profiles"  # also add pyinstrument to MIDDLEWARE

# Application definition


INSTALLED_APPS = [
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "custodia",
    "dst",
] + (["silk"] if SILK_ENABLED else [])

AUTH_USER_MODEL = "dst.User"
LOGIN_URL = "/custodia/login"

AUTHENTICATION_BACKENDS = ["demschooltools.auth.PlaySessionBackend"]

APPLICATION_SECRET = os.environ["APPLICATION_SECRET"]

REST_FRAMEWORK = {
    "DEFAULT_RENDERER_CLASSES": ("drf_orjson_renderer.renderers.ORJSONRenderer",),
    "DEFAULT_PARSER_CLASSES": ("drf_orjson_renderer.parsers.ORJSONParser",),
    "EXCEPTION_HANDLER": "rollbar.contrib.django_rest_framework.post_exception_handler",
}

MIDDLEWARE = (["silk.middleware.SilkyMiddleware"] if SILK_ENABLED else []) + [
    "django.middleware.security.SecurityMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "demschooltools.auth.PlaySessionMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "demschooltools.middleware.TimezoneMiddleware",
    # "pyinstrument.middleware.ProfilerMiddleware",
    "rollbar.contrib.django.middleware.RollbarNotifierMiddlewareExcluding404",
]

ROLLBAR_FRONTEND_TOKEN = os.environ.get("ROLLBAR_FRONTEND_TOKEN", "")
ROLLBAR_ENVIRONMENT = "development"
ROLLBAR = {
    "access_token": os.environ.get("ROLLBAR_TOKEN", ""),
    "branch": "main",
    "capture_username": True,
    "capture_email": True,
    "environment": ROLLBAR_ENVIRONMENT,
    "root": os.path.abspath(os.path.join(__file__, "..", "..")),
}

ROOT_URLCONF = "demschooltools.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [BASE_DIR / "demschooltools" / "templates"],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.debug",
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "demschooltools.wsgi.application"

LOGGING = {
    "version": 1,
    "disable_existing_loggers": True,
    "formatters": {
        "verbose": {
            "format": "%(levelname)-5s [%(asctime)s] %(name)-20s: %(message)s",
        },
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "verbose",
        },
    },
    "loggers": {
        # "rollbar": {
        #     "handlers": ["console"],
        #     "level": "DEBUG",
        # },
        "django": {
            "handlers": ["console"],
            "level": "INFO",
        },
        "demschooltools": {
            "handlers": ["console"],
            "level": "INFO",
        },
        "django.db.backends": {
            "handlers": ["console"],
            "level": "INFO",  # Change to DEBUG to see all DB queries
        },
    },
}

# Database
# https://docs.djangoproject.com/en/3.2/ref/settings/#databases

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": os.environ.get("DST_DB_NAME", "school_crm"),
        "PORT": "5432",
        "HOST": "localhost",
        "USER": "postgres",
        "PASSWORD": "123",
        "CONN_MAX_AGE": 60,
    },
}

USE_TZ = True
TIME_ZONE = "UTC"


# Password validation
# https://docs.djangoproject.com/en/3.2/ref/settings/#auth-password-validators

AUTH_PASSWORD_VALIDATORS = [
    {
        "NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator",
    },
    {
        "NAME": "django.contrib.auth.password_validation.MinimumLengthValidator",
    },
    {
        "NAME": "django.contrib.auth.password_validation.CommonPasswordValidator",
    },
    {
        "NAME": "django.contrib.auth.password_validation.NumericPasswordValidator",
    },
]

PASSWORD_HASHERS = [
    "django.contrib.auth.hashers.PBKDF2PasswordHasher",
    "django.contrib.auth.hashers.BCryptPasswordHasher",  # For reading passwords hashed by Custodia
]

# Internationalization
# https://docs.djangoproject.com/en/3.2/topics/i18n/

LANGUAGE_CODE = "en-us"

USE_I18N = True
USE_L10N = True


# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/3.2/howto/static-files/

STATIC_URL = "/django-static/"
STATICFILES_DIRS = [
    BASE_DIR / "static",
]
STORAGES = {
    "staticfiles": {"BACKEND": "demschooltools.storage.RandomVersionStaticFilesStorage"}
}

# Default primary key field type
# https://docs.djangoproject.com/en/3.2/ref/settings/#default-auto-field

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"
