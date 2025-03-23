"""
WSGI config for demschooltools project.

It exposes the WSGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/3.2/howto/deployment/wsgi/
"""

import os

from django.core.wsgi import get_wsgi_application

assert "DJANGO_SETTINGS_MODULE" in os.environ

application = get_wsgi_application()
