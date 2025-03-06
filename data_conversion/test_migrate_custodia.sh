set -ex

psql << EOF
\set ON_ERROR_STOP 1
DROP DATABASE school_crm;
CREATE DATABASE school_crm;
DROP ROLE custodia;
EOF

psql school_crm < ~/dump-school_crm-202502221543.sql
psql school_crm < ../data_conversion/migrate_custodia_to_django.sql

psql << EOF
INSERT INTO organization_hosts(host, organization_id)
    VALUES ('localhost:9000', 1) ON CONFLICT DO NOTHING;
EOF

uv run manage.py migrate custodia --fake
uv run manage.py migrate
