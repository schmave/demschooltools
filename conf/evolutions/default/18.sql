# --- !Ups

ALTER TABLE person_at_meeting ADD COLUMN id serial;

ALTER TABLE "case" ALTER meeting_id SET NOT NULL;
ALTER TABLE "case" ADD COLUMN location VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE "case" ADD COLUMN writer_id INTEGER;
ALTER TABLE "case" ADD constraint fk_case_writer foreign key (writer_id) references person(person_id);

# --- !Downs

ALTER TABLE person_at_meeting DROP COLUMN id;

ALTER TABLE "case" ALTER meeting_id DROP NOT NULL;
ALTER TABLE "case" DROP COLUMN location;
ALTER TABLE "case" DROP COLUMN writer_id;
ALTER TABLE "case" DROP constraint fk_case_writer;

