set -ex

psql << 'EOF'
\set ON_ERROR_STOP 1
DROP DATABASE IF EXISTS school_crm;
CREATE DATABASE school_crm;
--- DROP ROLE IF EXISTS custodia;
EOF

# ./manage.py migrate dst 0001
# ./manage.py migrate dst 0002 --fake
# ./manage.py migrate
# pg_dump -O --schema-only school_crm > orig_sql.sql

# ./manage.py migrate dst 0001 --fake
# ./manage.py migrate dst 0002
# ./manage.py migrate dst 0003 --fake
./manage.py migrate
pg_dump -O --schema-only school_crm > django_migrate.sql

uv run manage.py setup_initial_data
