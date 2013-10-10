# --- !Ups

CREATE TABLE testify_record
  (
    case_number VARCHAR(255) NOT NULL DEFAULT '',
    person_id int,
    constraint pk_testify_record primary key (case_number, person_id),
    constraint fk_testify_person foreign key (person_id) references person(person_id),
    constraint fk_testify_case foreign key (case_number) references "case"(case_number)
  );

ALTER TABLE charge ADD constraint fk_charge_case foreign key (case_Id) references "case"(case_number);

CREATE TABLE meeting
   (
    id serial,
    "date" DATE,
    constraint pk_meeting primary key (id)
    );

CREATE TABLE person_at_meeting
   (
    meeting_id int,
    person_id int,
    role int,
    constraint pk_person_at_meeting primary key (meeting_id, person_id, role),
    constraint fk_PAM_meeting foreign key (meeting_id) references meeting(id),
    constraint fk_PAM_person foreign key (person_id) references person(person_id)
    );

ALTER TABLE "case" ADD COLUMN meeting_id int;
ALTER TABLE "case" ADD constraint fk_case_meeting foreign key (meeting_id) references meeting(id);

# --- !Downs

DROP TABLE testify_record;
ALTER TABLE charge DROP constraint fk_charge_case;
DROP TABLE person_at_meeting;
DROP TABLE meeting;

ALTER TABLE "case" DROP constraint fk_case_meeting;
ALTER TABLE "case" DROP COLUMN meeting_id;

