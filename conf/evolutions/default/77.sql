# --- !Ups

ALTER TABLE "case" DROP COLUMN composite_findings;
ALTER TABLE charge ADD COLUMN rp_extension integer;

# --- !Downs

ALTER TABLE charge DROP COLUMN rp_extension;
ALTER TABLE "case" ADD COLUMN composite_findings text NOT NULL DEFAULT('');