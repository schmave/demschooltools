set -ex

psql << 'EOF'
\set ON_ERROR_STOP 1
DROP DATABASE IF EXISTS school_crm;
CREATE DATABASE school_crm;
--- DROP ROLE IF EXISTS custodia;
EOF


########
## Test coming from existing database

# pg_restore -d school_crm -U postgres -O -e ~/Downloads/backup_2025_11_12/db.dump
# # Fake apply dst/0001_initial and custodia/0003_swipe_person_swipe_day_empty_out_unique
# # Don't use ./manage.py migrate because it gets confused.
# psql school_crm << 'EOF'
# \set ON_ERROR_STOP 1
# INSERT INTO django_migrations(app, name, applied) VALUES
#     ('dst', '0001_initial', clock_timestamp()),
#     ('custodia', '0003_swipe_person_swipe_day_empty_out_unique', clock_timestamp());
# EOF
# ./manage.py migrate
# pg_dump -O --schema-only school_crm > orig_sql.sql


########
## Test starting from clean database

./manage.py migrate
pg_dump -O --schema-only school_crm > django_migrate.sql
uv run manage.py setup_initial_data
