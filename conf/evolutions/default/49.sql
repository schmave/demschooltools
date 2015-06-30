# --- !Ups

CREATE TABLE attendance_code
(
    id serial NOT NULL,
    organization_id integer NOT NULL,
    code    VARCHAR(8) NOT NULL,
    description text NOT NULL,
    color VARCHAR(16) NOT NULL,
    CONSTRAINT pk_attendance_code PRIMARY KEY(id),
    CONSTRAINT fk_attendance_code_organization FOREIGN KEY (organization_id) references organization(id)
);

CREATE TABLE attendance_day
(
    id serial NOT NULL,
    day DATE NOT NULL,
    person_id INTEGER NOT NULL,
    code VARCHAR(8),
    start_time time,
    end_time time,
    CONSTRAINT pk_attendance_day PRIMARY KEY(id),
    CONSTRAINT fk_attendance_day_person FOREIGN KEY (person_id) references person(person_id)
);

CREATE TABLE attendance_week
(
    id serial NOT NULL,
    person_id INTEGER NOT NULL,
    monday DATE NOT NULL,
    extra_hours INTEGER NOT NULL,
    CONSTRAINT pk_attendance_week PRIMARY KEY(id),
    CONSTRAINT fk_attendance_week_person FOREIGN KEY (person_id) references person(person_id)
);

# --- !Downs

DROP TABLE attendance_week;
DROP TABLE attendance_day;
DROP TABLE attendance_code;

