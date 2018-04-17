# --- !Ups

ALTER TABLE transactions ADD COLUMN archived boolean NOT NULL DEFAULT(false);

# --- !Downs

ALTER TABLE transactions DROP COLUMN archived;