CREATE SCHEMA overseer;

CREATE TABLE overseer.session_store (
session_id VARCHAR(36) NOT NULL PRIMARY KEY,
idle_timeout BIGINT,
absolute_timeout BIGINT,
value BYTEA
);
--;;
create table swipes(
  _id bigserial primary key,
  student_id bigserial,
  in_time timestamp  with time zone,
  inserted_date timestamp default now(),
  out_time timestamp with time zone
);
--;;
create table overrides(
_id bigserial primary key,
student_id bigserial,
inserted_date timestamp default now(),
date date
);
--;;
create table excuses(
_id bigserial primary key,
student_id bigserial,
inserted_date timestamp default now(),
date date
);
--;;
create table years(
  _id bigserial primary key,
  from_date timestamp  with time zone,
  to_date timestamp  with time zone,
  inserted_date timestamp default now(),
  name varchar(255)
);
--;;

create table students(
_id bigserial primary key,
name varchar(255),
inserted_date timestamp default now(),
olderdate date,
show_as_absent date
);
--;;

CREATE OR REPLACE VIEW roundedswipes AS
SELECT _id, in_time, out_time, student_id FROM swipes;
--;;

CREATE OR REPLACE FUNCTION school_days(year_name text)
RETURNS TABLE (days date, student_id BIGINT, olderdate date) AS
$func$
SELECT a.days, students._id student_id, students.olderdate FROM (SELECT DISTINCT days2.days
FROM (SELECT
(CASE WHEN date(s.in_time at time zone 'America/New_York')  IS NULL
THEN date(s.out_time at time zone 'America/New_York')
ELSE date(s.in_time at time zone 'America/New_York') END) as days
FROM roundedswipes s
INNER JOIN years y
ON ((s.out_time BETWEEN y.from_date AND y.to_date)
OR (s.in_time BETWEEN y.from_date AND y.to_date))
WHERE y.name= $1) days2
ORDER BY days2.days) as a
JOIN students on (1=1)
$func$
LANGUAGE sql;
