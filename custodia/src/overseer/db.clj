(ns overseer.db
  (:import
   [java.util Date Calendar TimeZone]
   [java.sql PreparedStatement])
  (:require [carica.core :as c]
            [yesql.core :refer [defqueries]]
            [cemerick.url :as url]
            [clojure.tools.trace :as trace]
            [heroku-database-url-to-jdbc.core :as h]
            [com.ashafa.clutch :as couch]
            [environ.core :refer [env]]
            [clj-time.coerce :as timec]
            [clj-time.format :as timef]
            [clojure.java.jdbc :as jdbc]
            ))

(defqueries "overseer/queries.sql")

(extend-type Date
  jdbc/ISQLParameter
  (set-parameter [val ^PreparedStatement stmt ix]
    (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
      (.setTimestamp stmt ix (timec/to-timestamp val) cal))))

(def create-school-days-function
  "
CREATE OR REPLACE FUNCTION school_days(year_name text)
  RETURNS TABLE (days date, student_id BIGINT, archived boolean, olderdate date) AS
$func$
  SELECT a.days, students._id student_id, students.archived, students.olderdate FROM (SELECT DISTINCT days2.days
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
  ")

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
   FROM swipes s;
")
(def create-rounded-swipes-view
  "
  CREATE OR REPLACE VIEW roundedswipes AS
  SELECT _id, in_time, out_time, student_id FROM swipes;
  ")

(def create-students-table-sql
  "create table students(
  _id bigserial primary key,
  name varchar(255),
  inserted_date timestamp default now(),
  olderdate date,
  show_as_absent date,
  archived BOOLEAN NOT NULL DEFAULT FALSE
  );")
(def create-swipes-table-sql
  "create table swipes(
  _id bigserial primary key,
  student_id bigserial,
  in_time timestamp  with time zone,
  inserted_date timestamp default now(),
  out_time timestamp with time zone
  );")
(def create-years-table-sql
  "create table years(
  _id bigserial primary key,
  from_date timestamp  with time zone,
  to_date timestamp  with time zone,
  inserted_date timestamp default now(),
  name varchar(255)
  );")

(def create-override-table-sql
  "create table overrides(
  _id bigserial primary key,
  student_id bigserial,
  inserted_date timestamp default now(),
  date date
  );")

(def create-excuses-table-sql
  "create table excuses(
  _id bigserial primary key,
  student_id bigserial,
  inserted_date timestamp default now(),
  date date
  );")

(def create-session-store-sql
  " CREATE TABLE session_store (
  session_id VARCHAR(36) NOT NULL PRIMARY KEY,
  idle_timeout BIGINT,
  absolute_timeout BIGINT,
  value BYTEA
  );
")

(def pgdb (atom nil))
;; (init-pg)
(defn init-pg []
  (swap! pgdb (fn [old]
                (dissoc (h/korma-connection-map (env :database-url))
                        :classname))))

(defn create-all-tables []
  (jdbc/execute! @pgdb [create-session-store-sql])
  (jdbc/execute! @pgdb [create-swipes-table-sql])
  (jdbc/execute! @pgdb [create-override-table-sql])
  (jdbc/execute! @pgdb [create-excuses-table-sql])
  (jdbc/execute! @pgdb [create-years-table-sql])
  (jdbc/execute! @pgdb [create-students-table-sql])
  (jdbc/execute! @pgdb [create-rounded-swipes-view])
  (jdbc/execute! @pgdb [create-school-days-function])
  )
(defn drop-all-tables []
  (jdbc/execute! @pgdb ["DROP FUNCTION IF EXISTS school_days(text);"])
  (jdbc/execute! @pgdb ["DROP VIEW IF EXISTS roundedswipes;"])
  (jdbc/execute! @pgdb ["DROP TABLE IF EXISTS swipes;"])
  (jdbc/execute! @pgdb ["DROP TABLE IF EXISTS session_store;"])
  (jdbc/execute! @pgdb ["DROP TABLE IF EXISTS students;"])
  (jdbc/execute! @pgdb ["DROP TABLE IF EXISTS excuses;"])
  (jdbc/execute! @pgdb ["DROP TABLE IF EXISTS overrides;"])
  (jdbc/execute! @pgdb ["DROP TABLE IF EXISTS years;"])
  )

(defn reset-db []
  (drop-all-tables) 
  (create-all-tables))

(defn delete! [doc]
  (let [table (:type doc)
        id (:_id doc)]
    (jdbc/delete! @pgdb table ["_id=?" id])))

(defn update! [table id fields]
  (let [fields (dissoc fields :type)]
    (jdbc/update! @pgdb table fields ["_id=?" id])))

(defn persist! [doc]
  (let [table (:type doc)
        doc (dissoc doc :type)]
    (first (map #(assoc % :type table)
                (jdbc/insert! @pgdb table doc)))))

;; (jdbc/query @pgdb ["select * from students"])
;; (jdbc/query @pgdb ["select * from students where id in (?)" "1"])
;; (persist! {:type :students :name "steve" :olderdate nil})
;; (update! :students 1 {:olderdate  "test"})
(defn get-overrides-in-year [year-name student-id]
  (get-overrides-in-year-y @pgdb year-name student-id))

(defn lookup-last-swipe [student-id]
  (first (lookup-last-swipe-y @pgdb  student-id)))

(defn get-excuses-in-year [year-name student-id]
  (get-excuses-in-year-y @pgdb year-name student-id))

(defn get-student-list-in-out [show-archived]
  (student-list-in-out-y @pgdb show-archived))

;; (map :student_id (get-student-list-in-out))

(defn get-school-days [year-name]
  (get-school-days-y @pgdb year-name))

;; (map :days (get-school-days "2014-06-01 2015-06-01"))
;; (get-student-page 7 "2014-07-23 2015-06-17")

(defn get-student-page [id year]
  (get-student-page-y @pgdb year id))

(defn get-report [year-name]
  (student-report-y @pgdb year-name))

;; (get-overrides-in-year "2014-06-01 2015-06-01" 3 )
;; (get-report "2014-06-01 2015-06-01" )  
;;  (count (get-school-days "2014-06-01 2015-06-01" ))  
;; (def t (get-report "2014-06-01 2015-06-01" ))
;; (count t)

(defn get-swipes-in-year [year-name student-id]
  (swipes-in-year-y @pgdb year-name student-id)
  )

;; (get-swipes-in-year "2014-06-01 2015-06-01" 1)

(defn get-*
  ([type] (map #(assoc % :type (keyword type))
               (jdbc/query @pgdb [(str "select * from " type " order by inserted_date ")])))
  ([type id id-col]
     (map #(assoc % :type (keyword type))
          (if id
            (jdbc/query @pgdb [(str "select * from " type
                                    " where " id-col "=?"
                                    " order by inserted_date")
                               id])
            (jdbc/query @pgdb [(str "select * from " type " order by inserted_date")])))))

