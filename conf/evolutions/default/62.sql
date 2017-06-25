# --- !Ups

ALTER TABLE organization ADD COLUMN jc_reset_day int DEFAULT 3 NOT NULL;

# --- !Downs

ALTER TABLE organization DROP COLUMN jc_reset_day;
