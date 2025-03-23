#!/bin/bash

set -x

PID_FILE=../dst-django.pid

if [ -f $PID_FILE ]; then
    kill $(cat $PID_FILE)
    sleep 5
fi

if [ -f $PID_FILE ]; then
    kill -9 $(cat $PID_FILE)
    sleep 3
fi

if [ -f $PID_FILE ]; then
    rm $PID_FILE
fi

DJANGO_SETTINGS_MODULE=demschooltools.settings_prod \
    uv run --group prod gunicorn --threads 4 --pid $PID_FILE demschooltools.wsgi
