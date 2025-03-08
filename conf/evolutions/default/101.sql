
# --- !Ups

-- Prepare for migrating custodia columns to DST
ALTER TABLE organization ADD COLUMN	timezone varchar(255) DEFAULT 'America/New_York' NOT NULL;
ALTER TABLE organization ADD COLUMN late_time TIME DEFAULT '10:15:00'::time without time zone NULL;
ALTER TABLE organization ALTER COLUMN late_time DROP DEFAULT;
ALTER TABLE person ADD COLUMN custodia_show_as_absent date NULL;
ALTER TABLE person ADD COLUMN custodia_start_date date NULL;

--- Add some columns for Django user stuff
ALTER TABLE users ADD COLUMN date_joined timestamptz NOT NULL default '2000-01-01';
ALTER TABLE users ADD COLUMN last_login timestamptz NULL default NULL;
ALTER TABLE users ADD COLUMN first_name varchar(150) NOT NULL default '';
ALTER TABLE users ADD COLUMN last_name varchar(150) NOT NULL default '';
ALTER TABLE users ADD COLUMN is_superuser bool NOT NULL default false;
ALTER TABLE users ADD COLUMN is_staff bool NOT NULL default false;
ALTER TABLE users ADD COLUMN username varchar(150) NOT NULL default '';

--- Add primary keys where there were none
ALTER TABLE person_change ADD COLUMN id SERIAL PRIMARY KEY;


# --- !Downs

ALTER TABLE organization DROP COLUMN timezone;
ALTER TABLE organization DROP COLUMN late_time;
ALTER TABLE person DROP COLUMN custodia_show_as_absent;
ALTER TABLE person DROP COLUMN custodia_start_date;

ALTER TABLE USERS drop column date_joined;
ALTER TABLE USERS drop column last_login;
ALTER TABLE USERS drop column first_name;
ALTER TABLE USERS drop column last_name;
ALTER TABLE USERS drop column is_superuser;
ALTER TABLE USERS drop column is_staff;
ALTER TABLE USERS drop column username;
