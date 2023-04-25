# --- !Ups

CREATE TABLE attendance_rule (
   id serial,
   organization_id integer NOT NULL,
   person_id integer,
   start_date date,
   end_date date,
   notification_email text,
   expired boolean NOT NULL,
   days_of_week integer NOT NULL,
   absence_code VARCHAR(64),
   min_hours double precision,
   latest_start_time time,
   latest_departure_time time,
   CONSTRAINT pk_attendance_rule PRIMARY KEY(id),
   CONSTRAINT fk_attendance_rule_organization FOREIGN KEY (organization_id) references organization(id),
   CONSTRAINT fk_attendance_rule_person FOREIGN KEY (person_id) references person(person_id)
);

# --- !Downs

DROP TABLE attendance_rule;