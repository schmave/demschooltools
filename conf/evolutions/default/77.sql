# --- !Ups

ALTER TABLE "case" DROP COLUMN composite_findings;

# --- !Downs

ALTER TABLE "case" ADD COLUMN composite_findings text NOT NULL DEFAULT('');