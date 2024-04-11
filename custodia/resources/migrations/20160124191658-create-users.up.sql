-- This was "users" in the public schema, but that's not compatible with
-- DemSchoolTools. Make this phillyfreeschool for now, and it will be moved
-- to the overseer schem later.
create schema phillyfreeschool;

--;;

CREATE TABLE phillyfreeschool.users(
  user_id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255),
  password VARCHAR(255),
  roles VARCHAR(255),
  schema_name VARCHAR(255),
  inserted_date TIMESTAMP DEFAULT NOW()
);
