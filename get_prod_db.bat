set PG_PATH=\Program Files\PostgreSQL\9.4\bin\

"%PG_PATH%pg_dump" -p 5433 -U evan -f school_crm.dump -Fc school_crm

"%PG_PATH%pg_restore" -d school_crm -U postgres -W -e school_crm.dump

"%PG_PATH%psql" -W -c "INSERT INTO organization_hosts VALUES ('localhost:9000', 1);DELETE FROM overseer.users;" school_crm postgres
