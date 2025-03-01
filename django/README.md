This project uses [uv](https://docs.astral.sh/uv/) to manage its dependencies. Install it using the standalone installer [described here](https://docs.astral.sh/uv/getting-started/installation/#standalone-installer).

To run the dev server:

    uv run manage.py runserver

You'll also need to follow the instructions in custodia/README.md to build the frontend

To run a script:

    uv run manage.py runscript print_stats --script-args 2024
