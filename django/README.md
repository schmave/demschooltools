To set up virtual environment:

    python3 -m pip install -U uv
    uv venv
    source .venv/bin/activate
    uv pip install -r requirements.txt

To run a script:

    source .venv/bin/activate
    ./manage.py runscript print_stats --script-args 2024
