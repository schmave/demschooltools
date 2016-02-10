# --- !Ups

ALTER TABLE person_at_meeting ADD COLUMN id serial;

ALTER TABLE "case" ALTER meeting_id SET NOT NULL;
ALTER TABLE "case" ADD COLUMN location VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE "case" ADD COLUMN writer_id INTEGER;
ALTER TABLE "case" ADD constraint fk_case_writer foreign key (writer_id) references person(person_id);

UPDATE "case" SET findings='' WHERE findings IS NULL;
ALTER TABLE "case" ALTER findings SET NOT NULL;

ALTER TABLE "testify_record" ADD COLUMN id serial;

UPDATE charge SET resolution_plan='' WHERE resolution_plan IS NULL;
ALTER TABLE charge ALTER resolution_plan SET NOT NULL;

# --- !Downs

ALTER TABLE person_at_meeting DROP COLUMN id;

ALTER TABLE "case" ALTER meeting_id DROP NOT NULL;
ALTER TABLE "case" DROP COLUMN location;
ALTER TABLE "case" DROP COLUMN writer_id;
ALTER TABLE "case" ALTER findings DROP NOT NULL;

ALTER TABLE "testify_record" DROP COLUMN id;

ALTER TABLE charge ALTER resolution_plan DROP NOT NULL;
