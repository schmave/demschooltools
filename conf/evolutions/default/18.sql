# --- !Ups

ALTER TABLE "person" ADD COLUMN grade varchar(8) not null default '';

# --- !Downs

ALTER TABLE "person" DROP COLUMN grade;

