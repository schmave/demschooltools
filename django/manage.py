#!/usr/bin/env python
"""Django's command-line utility for administrative tasks."""

import os
import sys

ENVIRONMENT_VARIABLE = "DJANGO_SETTINGS_MODULE"


def main():
    """Run administrative tasks."""
    if ENVIRONMENT_VARIABLE not in os.environ:
        module = "demschooltools.settings"
        os.environ[ENVIRONMENT_VARIABLE] = module
        print(f"\n[manage.py using default {ENVIRONMENT_VARIABLE}={module}]\n")

    try:
        from django.core.management import execute_from_command_line
    except ImportError as exc:
        raise ImportError(
            "Couldn't import Django. Are you sure it's installed and "
            "available on your PYTHONPATH environment variable? Did you "
            "forget to activate a virtual environment?"
        ) from exc
    execute_from_command_line(sys.argv)


if __name__ == "__main__":
    main()
