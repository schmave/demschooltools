set -ex

psql << 'EOF'
\set ON_ERROR_STOP 1
DROP DATABASE IF EXISTS school_crm;
CREATE DATABASE school_crm;
DROP ROLE IF EXISTS custodia;
EOF

psql school_crm < ~/dump-school_crm-202502221543.sql
psql school_crm < ../conf/evolutions/default/101.sql
psql school_crm < ../data_conversion/migrate_custodia_to_django.sql

psql school_crm << 'EOF'
\set ON_ERROR_STOP 1
INSERT INTO organization_hosts(host, organization_id)
    VALUES ('localhost:9000', 1) ON CONFLICT DO NOTHING;
EOF

uv run manage.py migrate custodia --fake
uv run manage.py migrate
