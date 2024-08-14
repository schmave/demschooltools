# --- !Ups

ALTER TABLE tag ADD COLUMN show_in_roles boolean NOT NULL DEFAULT true;

# --- !Downs

ALTER TABLE tag DROP COLUMN show_in_roles;