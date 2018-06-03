create schema demo;
  create table demo.students(
    _id bigserial primary key,
    name varchar(255),
    inserted_date timestamp default now(),
    olderdate date,
    show_as_absent date,
    archived BOOLEAN NOT NULL DEFAULT FALSE
  );

  create table demo.swipes(
    _id bigserial primary key,
    student_id bigserial,
    in_time timestamp  with time zone,
    out_time timestamp with time zone
  );

   create table demo.overrides(
    _id bigserial primary key,
    student_id bigserial,
    inserted_date timestamp default now(),
    date date
  );
  create table demo.excuses(
    _id bigserial primary key,
    student_id bigserial,
    inserted_date timestamp default now(),
    date date
  );

  CREATE TABLE session_store (
    session_id VARCHAR(36) NOT NULL PRIMARY KEY,
    idle_timeout BIGINT,
    absolute_timeout BIGINT,
    value BYTEA
  );

  CREATE OR REPLACE VIEW demo.roundedswipes AS
  SELECT _id, in_time, out_time, student_id FROM demo.swipes;

  CREATE TABLE demo.classes(
       _id BIGSERIAL PRIMARY KEY,
       name VARCHAR(255),
       inserted_date timestamp default now(),
       active BOOLEAN NOT NULL DEFAULT FALSE
  );

  CREATE TABLE demo.Classes_X_students(
       class_id BIGINT NOT NULL REFERENCES demo.classes(_id),
       student_id BIGINT NOT NULL REFERENCES demo.students(_id)
  );

  create table demo.years(
    _id bigserial primary key,
    from_date timestamp  with time zone,
    to_date timestamp  with time zone,
    inserted_date timestamp default now(),
    name varchar(255)
  );

CREATE OR REPLACE FUNCTION demo.school_days(year_name TEXT, class_id BIGINT)
  RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS
$func$

SELECT a.days, s._id student_id, s.archived, s.olderdate
FROM (SELECT DISTINCT days2.days
    FROM (SELECT
            (CASE WHEN date(s.in_time AT TIME ZONE 'America/New_York')  IS NULL
            THEN date(s.out_time AT TIME ZONE 'America/New_York')
            ELSE date(s.in_time AT TIME ZONE 'America/New_York') END) AS days
         FROM demo.roundedswipes s
         INNER JOIN demo.years y
            ON ((s.out_time BETWEEN y.from_date AND y.to_date)
            OR (s.in_time BETWEEN y.from_date AND y.to_date))
         JOIN demo.classes c ON (c.active = true)
         JOIN demo.classes_X_students cXs ON (cXs.class_id = c._id
                                         AND s.student_id = cXs.student_id)
         WHERE y.name = $1) days2
         ORDER BY days2.days) AS a
JOIN demo.classes_X_students cXs ON (1=1)
JOIN demo.students s ON (s._id = cXs.student_id)
WHERE cXs.class_id = $2
$func$
LANGUAGE sql;
