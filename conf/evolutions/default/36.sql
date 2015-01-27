# --- !Ups

ALTER TABLE "case" ADD COLUMN time VARCHAR(255) NOT NULL DEFAULT '';

# --- !Downs

ALTER TABLE "case" DROP COLUMN time;

