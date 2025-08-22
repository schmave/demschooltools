set -x

. ../set_env.sh

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


export DJANGO_SETTINGS_MODULE="demschooltools.settings_prod"

uv run manage.py migrate
uv run manage.py collectstatic --noinput
uv run playwright install chromium
nohup uv run --group prod gunicorn --threads 4 --pid $PID_FILE demschooltools.wsgi >> ../dst-django.log 2>&1 &
