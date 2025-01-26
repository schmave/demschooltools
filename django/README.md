To set up virtual environment:

    python3.12 -m pip install -U uv
    python3.12 -m uv venv venv
    source venv/bin/activate
    uv pip install -r requirements.txt

To run the dev server:

    source .venv/bin/activate
    ./manage.py runserver

    You'll also need to follow the instructions in custodia/README.md to
    build the frontend

To run a script:

    source .venv/bin/activate
    ./manage.py runscript print_stats --script-args 2024
