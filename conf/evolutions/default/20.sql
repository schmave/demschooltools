# --- !Ups

ALTER TABLE person add column display_name varchar(255) not null default '';

# --- !Downs

ALTER TABLE person drop column display_name;
