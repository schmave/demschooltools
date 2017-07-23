ALTER TABLE overseer.users set schema phillyfreeschool;
--;;

DROP SCHEMA IF EXISTS overseer CASCADE;

--;;

ALTER SCHEMA phillyfreeschool RENAME TO overseer;

--;;

DROP SCHEMA IF EXISTS phillyfreeschool CASCADE;
