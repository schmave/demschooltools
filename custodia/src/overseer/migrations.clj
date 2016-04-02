(ns overseer.migrations
  (:require [environ.core :refer [env]]
            [migratus.core :as migratus]
            [clojure.java.io :as io]))

(defn replace-philly [from with]
  (clojure.string/replace from #"phillyfreeschool" with))

(defn make-queries [name]
  (let [data (slurp "src/overseer/queries/phillyfreeschool.sql")
        data (replace-philly data name)]
    (with-open [wrtr (io/writer (str "src/overseer/queries/" name ".sql"))]
      (.write wrtr data))))


(comment "
 -- broken till otherwise noted
  CREATE VIEW roundedswipes AS
  SELECT
     _id
     , ((CASE WHEN (EXTRACT(HOURS FROM s.in_time AT TIME ZONE 'America/New_York') < 9
                  AND EXTRACT(HOURS FROM s.in_time AT TIME ZONE 'America/New_York') < 16)
              THEN (date_trunc('day', s.in_time AT TIME ZONE 'America/New_York') + interval '9 hours')
              WHEN (EXTRACT(HOURS FROM s.in_time AT TIME ZONE 'America/New_York') >= 16)
              THEN (date_trunc('day', s.in_time AT TIME ZONE 'America/New_York') + interval '16 hours')
          ELSE s.in_time END)) as in_time
     , ((CASE WHEN (EXTRACT(HOURS FROM s.out_time AT TIME ZONE 'America/New_York') >= 16)
              THEN (date_trunc('day', s.out_time AT TIME ZONE 'America/New_York') + interval '16 hours') 
              WHEN (EXTRACT(HOURS FROM s.out_time AT TIME ZONE 'America/New_York') < 9)
              THEN (date_trunc('day', s.out_time AT TIME ZONE 'America/New_York') + interval '9 hours')
          ELSE s.out_time END)) AS out_time
     , student_id
   FROM phillyfreeschool.swipes s;
")

(def initialize-shared-tables "
  CREATE TABLE users(
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255),
    password VARCHAR(255),
    roles VARCHAR(255),
    schema_name VARCHAR(255),
    inserted_date TIMESTAMP DEFAULT NOW()
  );

  CREATE TABLE session_store (
    session_id VARCHAR(36) NOT NULL PRIMARY KEY,
    idle_timeout BIGINT,
    absolute_timeout BIGINT,
    value BYTEA
  );
  ")
(def create-philly-schema-sql
  "
  create table phillyfreeschool.students(
    _id bigserial primary key,
    name varchar(255),
    inserted_date timestamp default now(),
    olderdate date,
    start_date date,
    show_as_absent date,
    archived BOOLEAN NOT NULL DEFAULT FALSE
  );

  create table phillyfreeschool.swipes(
    _id bigserial primary key,
    student_id bigserial,
    in_time timestamp  with time zone,
    inserted_date timestamp default now(),
    out_time timestamp with time zone
  );

   create table phillyfreeschool.overrides(
    _id bigserial primary key,
    student_id bigserial,
    inserted_date timestamp default now(),
    date date
  );
  create table phillyfreeschool.excuses(
    _id bigserial primary key,
    student_id bigserial,
    inserted_date timestamp default now(),
    date date
  );


  CREATE OR REPLACE VIEW phillyfreeschool.roundedswipes AS
  SELECT _id, in_time, out_time, student_id FROM phillyfreeschool.swipes;

  CREATE TABLE phillyfreeschool.classes(
       _id BIGSERIAL PRIMARY KEY,
       name VARCHAR(255),
       inserted_date timestamp default now(),
       active BOOLEAN NOT NULL DEFAULT FALSE
  );

  CREATE TABLE phillyfreeschool.Classes_X_students(
       class_id BIGINT NOT NULL REFERENCES phillyfreeschool.classes(_id),
       student_id BIGINT NOT NULL REFERENCES phillyfreeschool.students(_id)
  );

  create table phillyfreeschool.years(
    _id bigserial primary key,
    from_date timestamp  with time zone,
    to_date timestamp  with time zone,
    inserted_date timestamp default now(),
    name varchar(255)
  );

CREATE OR REPLACE FUNCTION phillyfreeschool.school_days(year_name TEXT, class_id BIGINT)
  RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS
$func$

SELECT a.days, s._id student_id, s.archived, s.olderdate
FROM (SELECT DISTINCT days2.days
    FROM (SELECT
            (CASE WHEN date(s.in_time AT TIME ZONE 'America/New_York')  IS NULL
            THEN date(s.out_time AT TIME ZONE 'America/New_York')
            ELSE date(s.in_time AT TIME ZONE 'America/New_York') END) AS days
         FROM phillyfreeschool.roundedswipes s
         INNER JOIN phillyfreeschool.years y
            ON ((s.out_time BETWEEN y.from_date AND y.to_date)
            OR (s.in_time BETWEEN y.from_date AND y.to_date))
         JOIN phillyfreeschool.classes c ON (c.active = true)
         JOIN phillyfreeschool.classes_X_students cXs ON (cXs.class_id = c._id
                                         AND s.student_id = cXs.student_id)
         WHERE y.name = $1) days2
         ORDER BY days2.days) AS a
JOIN phillyfreeschool.classes_X_students cXs ON (1=1)
JOIN phillyfreeschool.students s ON (s._id = cXs.student_id)
WHERE cXs.class_id = $2
AND (s.start_date < a.days OR s.start_date is null);
$func$
LANGUAGE sql;
  ")

(defn migrate-db [con]
  (migratus/migrate {:store :database
                     :db con}))

;; (migratus/create mconfig "student start date")
;; (migratus/migrate mconfig)
;; (migratus/rollback mconfig)
;; (migratus/down mconfig 20150908103000 20150909070853 20150913085152)

