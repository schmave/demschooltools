# --- !Ups

alter table person add column previous_school character varying(255) default '' not null;
alter table person add column school_district character varying(255) default '' not null;

# --- !Downs

alter table person drop column previous_school;
alter table person drop column school_district;

