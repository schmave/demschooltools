# --- !Ups

ALTER TABLE transactions ADD COLUMN created_by_user_id integer;

# --- !Downs

ALTER TABLE transactions DROP COLUMN created_by_user_id;