# --- !Ups

ALTER TABLE organization ADD COLUMN show_custodia BOOLEAN default false not null;
ALTER TABLE organization ADD COLUMN show_attendance BOOLEAN default true not null;
ALTER TABLE organization ADD COLUMN short_name VARCHAR(255) default '' not null;
ALTER TABLE organization ADD COLUMN custodia_password VARCHAR(255) default '' not null;

update organization set short_name='TRVS' where id=1;
update organization set short_name='PFS' where id=2;
update organization set short_name='Fairhaven' where id=3;
update organization set short_name='TCS' where id=4;
update organization set short_name='MLC' where id=5;
update organization set short_name='TOS' where id=6;
update organization set short_name='HSS' where id=7;
update organization set short_name='CSS' where id=8;
update organization set short_name='Sandbox' where id=9;


# --- !Downs

ALTER TABLE organization DROP COLUMN show_custodia;
ALTER TABLE organization DROP COLUMN show_attendance;
ALTER TABLE organization DROP COLUMN short_name;
ALTER TABLE organization DROP COLUMN custodia_password;
