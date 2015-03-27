# --- !Ups

ALTER TABLE testify_record ADD COLUMN role INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE testify_record RENAME TO person_at_case;

INSERT INTO person_at_case (person_id, case_id, role)
  (SELECT writer_id, id, 1 FROM "case" WHERE writer_id IS NOT NULL);

ALTER TABLE "case" DROP constraint fk_case_writer;
ALTER TABLE "case" DROP writer_id;

# --- !Downs

ALTER TABLE "case" ADD COLUMN writer_id INTEGER;
ALTER TABLE "case" ADD constraint fk_case_writer FOREIGN KEY (writer_id) REFERENCES person (person_id);

--- We could throw away some data here if there was more than
--- one writer. In the old world we can only record one.
UPDATE "case" c
  SET writer_id = pac.person_id
  FROM person_at_case pac
  WHERE c.id=pac.case_id AND pac.role=1;

ALTER TABLE person_at_case RENAME TO testify_record;

DELETE FROM testify_record WHERE role != 0;
ALTER TABLE testify_record DROP COLUMN role;