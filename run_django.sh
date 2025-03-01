#!/bin/bash

uv run gunicorn --threads 4 --pid ../dst-django.pid demschooltools.wsgi