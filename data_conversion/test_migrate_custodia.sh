set -ex

psql << EOF
\set ON_ERROR_STOP 1
DROP DATABASE school_crm;
CREATE DATABASE school_crm;
EOF

psql school_crm < ~/dump-school_crm-202502221543.sql
psql school_crm < ../data_conversion/migrate_custodia_to_django.sql
uv run manage.py migrate custodia --fake
uv run manage.py migrate
