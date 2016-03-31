create schema demo;
--;;
create table demo.students(
  _id bigserial primary key,
  name varchar(255),
  inserted_date timestamp default now(),
  olderdate date,
  show_as_absent date,
  archived BOOLEAN NOT NULL DEFAULT FALSE
);
--;;

create table demo.swipes(
  _id bigserial primary key,
  student_id bigserial,
  in_time timestamp  with time zone,
  inserted_date timestamp default now(),
  out_time timestamp with time zone
);
--;;

create table demo.overrides(
  _id bigserial primary key,
  student_id bigserial,
  inserted_date timestamp default now(),
  date date
);
--;;
create table demo.excuses(
  _id bigserial primary key,
  student_id bigserial,
  inserted_date timestamp default now(),
  date date
);
--;;

CREATE TABLE session_store (
  session_id VARCHAR(36) NOT NULL PRIMARY KEY,
  idle_timeout BIGINT,
  absolute_timeout BIGINT,
  value BYTEA
);
--;;

CREATE OR REPLACE VIEW demo.roundedswipes AS
SELECT _id, in_time, out_time, student_id FROM demo.swipes;
--;;

CREATE TABLE demo.classes(
      _id BIGSERIAL PRIMARY KEY,
      name VARCHAR(255),
      inserted_date timestamp default now(),
      active BOOLEAN NOT NULL DEFAULT FALSE
);
--;;

CREATE TABLE demo.Classes_X_students(
      class_id BIGINT NOT NULL REFERENCES demo.classes(_id),
      student_id BIGINT NOT NULL REFERENCES demo.students(_id)
);
--;;

create table demo.years(
  _id bigserial primary key,
  from_date timestamp  with time zone,
  to_date timestamp  with time zone,
  inserted_date timestamp default now(),
  name varchar(255)
);
--;;

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
--;;

insert into demo.classes (_id, name, active) values (1, '2014-2015', true);
--;;

insert into demo.students(_id, name) values (1, 'Jim');
--;;
insert into demo.students(_id, name) values (2, 'Sally');
--;;
insert into demo.students(_id, name) values (3, 'Jose');
--;;
insert into demo.students(_id, name) values (4, 'June');
--;;
insert into demo.students(_id, name) values (5, 'Bob');
--;;
insert into demo.students(_id, name) values (6, 'Beth');
--;;
insert into demo.students(_id, name) values (7, 'Rachel');
--;;

insert into demo.classes_x_students(class_id, student_id) values (1, 1);
--;;
insert into demo.classes_x_students(class_id, student_id) values (1, 2);
--;;
insert into demo.classes_x_students(class_id, student_id) values (1, 3);
--;;
insert into demo.classes_x_students(class_id, student_id) values (1, 4);
--;;
insert into demo.classes_x_students(class_id, student_id) values (1, 5);
--;;
insert into demo.classes_x_students(class_id, student_id) values (1, 6);
--;;
insert into demo.classes_x_students(class_id, student_id) values (1, 7);
--;;

delete from demo.years;
--;;
insert into demo.years(from_date, to_date, name) values
('2015-06-01 20:00:00-04', '2016-12-12 13:22:21.754519', '2015-06-01 2016-12-12');
--;;

delete from demo.swipes;
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (1, '2016-01-25 09:22:21.796-05', '2016-01-25 14:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (1, '2016-01-26 09:05:21.796-05', '2016-01-26 15:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (1, '2016-01-27 09:12:21.796-05', '2016-01-27 13:13:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (1, '2016-01-28 09:22:21.796-05', '2016-01-28 14:55:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (1, '2016-01-29 09:00:21.796-05', '2016-01-29 13:54:21.804-5');
--;;

insert into demo.swipes (student_id, in_time, out_time)
values (2, '2016-01-25 09:22:21.796-05', '2016-01-25 14:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (2, '2016-01-26 09:05:21.796-05', '2016-01-26 15:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (2, '2016-01-27 09:12:21.796-05', '2016-01-27 13:13:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (2, '2016-01-28 09:22:21.796-05', '2016-01-28 14:55:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (2, '2016-01-29 09:00:21.796-05', '2016-01-29 13:54:21.804-5');
--;;

insert into demo.swipes (student_id, in_time, out_time)
values (3, '2016-01-25 09:22:21.796-05', '2016-01-25 14:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (3, '2016-01-26 09:05:21.796-05', '2016-01-26 15:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (3, '2016-01-27 09:12:21.796-05', '2016-01-27 13:13:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (3, '2016-01-28 09:22:21.796-05', '2016-01-28 14:55:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (3, '2016-01-29 09:00:21.796-05', '2016-01-29 13:54:21.804-5');
--;;

insert into demo.swipes (student_id, in_time, out_time)
values (4, '2016-01-25 09:22:21.796-05', '2016-01-25 14:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (4, '2016-01-26 09:05:21.796-05', '2016-01-26 15:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (4, '2016-01-27 09:12:21.796-05', '2016-01-27 13:13:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (4, '2016-01-28 09:22:21.796-05', '2016-01-28 14:55:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (4, '2016-01-29 09:00:21.796-05', '2016-01-29 13:54:21.804-5');
--;;

insert into demo.swipes (student_id, in_time, out_time)
values (5, '2016-01-25 09:22:21.796-05', '2016-01-25 14:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (5, '2016-01-26 09:05:21.796-05', '2016-01-26 15:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (5, '2016-01-27 09:12:21.796-05', '2016-01-27 13:13:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (5, '2016-01-28 09:22:21.796-05', '2016-01-28 14:55:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (5, '2016-01-29 09:00:21.796-05', '2016-01-29 13:54:21.804-5');
--;;

insert into demo.swipes (student_id, in_time, out_time)
values (6, '2016-01-25 09:22:21.796-05', '2016-01-25 14:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (6, '2016-01-26 09:05:21.796-05', '2016-01-26 15:22:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (6, '2016-01-27 09:12:21.796-05', '2016-01-27 13:13:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (6, '2016-01-28 09:22:21.796-05', '2016-01-28 14:55:21.804-5');
--;;
insert into demo.swipes (student_id, in_time, out_time)
values (6, '2016-01-29 09:00:21.796-05', '2016-01-29 13:54:21.804-5');
--;;

select * from phillyfreeschool.years;
--;;
select * from phillyfreeschool.swipes;
--;;
select * from demo.swipes;
--;;
select * from demo.years;
--;;
select * from demo.classes;
--;;
select * from demo.students;
--;;
select * from demo.classes_x_students;
