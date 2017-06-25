# --- !Ups

ALTER TABLE organization ADD COLUMN show_history_in_print boolean DEFAULT true NOT NULL;
ALTER TABLE organization ADD COLUMN show_last_modified_in_print boolean DEFAULT true NOT NULL;

# --- !Downs

ALTER TABLE organization DROP COLUMN show_history_in_print;
ALTER TABLE organization DROP COLUMN show_last_modified_in_print;
