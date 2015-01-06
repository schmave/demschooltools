# --- !Ups

ALTER TABLE "case" ADD COLUMN id serial;

ALTER TABLE charge RENAME case_id TO case_number;

ALTER TABLE testify_record ADD COLUMN case_id INTEGER;
ALTER TABLE charge ADD COLUMN case_id INTEGER;

UPDATE testify_record tr SET case_id=(SELECT id FROM "case" c WHERE tr.case_number = c.case_number);
UPDATE charge cg SET case_id=(SELECT id FROM "case" c WHERE cg.case_number = c.case_number);

ALTER TABLE testify_record DROP constraint fk_testify_case;
ALTER TABLE testify_record DROP COLUMN case_number;
ALTER TABLE charge DROP constraint fk_charge_case;
ALTER TABLE charge DROP COLUMN case_number;

ALTER TABLE "case" DROP constraint pk_case;
ALTER TABLE "case" ADD constraint pk_case_2 primary key (id);
ALTER TABLE "case" ADD constraint u_case_number_meeting UNIQUE (case_number, meeting_id);

ALTER TABLE testify_record ADD CONSTRAINT fk_testify_case FOREIGN KEY (case_id) REFERENCES "case" (id);
ALTER TABLE charge ADD CONSTRAINT fk_charge_case FOREIGN KEY (case_id) REFERENCES "case" (id);

# --- !Downs

-- ! There is no down from here. Sorry. You're on your own.

