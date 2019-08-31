# --- !Ups
ALTER TABLE organization ADD COLUMN show_electronic_signin boolean NOT NULL DEFAULT false;

# --- !Downs
ALTER TABLE organization DROP COLUMN show_electronic_signin;
