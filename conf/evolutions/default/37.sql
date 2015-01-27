# --- !Ups

ALTER TABLE "case" ADD COLUMN severity VARCHAR(255) NOT NULL DEFAULT '';

# --- !Downs

ALTER TABLE "case" DROP COLUMN severity;

