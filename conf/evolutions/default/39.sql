# --- !Ups

ALTER TABLE "charge" ADD COLUMN severity VARCHAR(255) NOT NULL DEFAULT '';
alter table "case" drop column severity;

# --- !Downs

ALTER TABLE "charge" DROP COLUMN severity;
ALTER TABLE "case" ADD COLUMN severity VARCHAR(255) NOT NULL DEFAULT '';
