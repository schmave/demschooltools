#!/bin/bash

uv run --group prod gunicorn --threads 4 --pid ../dst-django.pid demschooltools.wsgi