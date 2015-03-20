(ns clojure-getting-started.db
  (:require [carica.core :as c]
            [cemerick.url :as url]
            [clojure.tools.trace :as trace]
            [heroku-database-url-to-jdbc.core :as h]
            [com.ashafa.clutch :as couch]
            [environ.core :refer [env]]
            [clj-time.coerce :as timec]
            [clj-time.format :as timef]
            [clojure.java.jdbc :as jdbc]
            ))

(def create-students-table-sql
  "create table students(
  _id bigserial primary key,
  name varchar(255),
  inserted_date timestamp default now(),
  olderdate varchar(255)
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
  date timestamp 
  );")

(def create-excuses-table-sql
  "create table excuses(
  _id bigserial primary key,
  student_id bigserial,
  inserted_date timestamp default now(),
  date timestamp 
  );")

(def pgdb
  (dissoc (h/korma-connection-map (env :database-url))
          :classname))

(defn create-all-tables []
  (jdbc/execute! pgdb [create-swipes-table-sql])
  (jdbc/execute! pgdb [create-override-table-sql])
  (jdbc/execute! pgdb [create-excuses-table-sql])
  (jdbc/execute! pgdb [create-years-table-sql])
  (jdbc/execute! pgdb [create-students-table-sql]))
(defn drop-all-tables []
  (jdbc/execute! pgdb ["drop table if exists students;"])
  (jdbc/execute! pgdb ["drop table if exists excuses;"])
  (jdbc/execute! pgdb ["drop table if exists overrides;"])
  (jdbc/execute! pgdb ["drop table if exists years;"])
  (jdbc/execute! pgdb ["drop table if exists swipes;"]))

(defn reset-db []
  (drop-all-tables)
  (create-all-tables))

(defn delete! [doc]
  (let [table (:type doc)
        id (:_id doc)]
    (jdbc/delete! pgdb table ["_id=?" id])))

(defn update! [table id fields]
  (let [fields (dissoc fields :type)]
    (jdbc/update! pgdb table fields ["_id=?" id])))

(defn persist! [doc]
  (let [table (:type doc)
        doc (dissoc doc :type)]
    (first (map #(assoc % :type table)
                (jdbc/insert! pgdb table doc)))))

;; (jdbc/query pgdb ["select * from students"])
;; (jdbc/query pgdb ["select * from students where id in (?)" "1"])
;; (persist! {:type :students :name "steve" :olderdate nil})
;; (update! :students 1 {:olderdate  "test"})
(defn get-overrides-in-year [year-name student-id]
  (let [q (str "
select e.*
       ,'overrides' as type
from overrides e
inner join years y 
ON (e.date BETWEEN y.from_date AND y.to_date)
where y.name=? AND e.student_id =?
")]
    (jdbc/query
     pgdb
     [q year-name student-id])))

(defn get-excuses-in-year [year-name student-id]
  (let [q (str "
select e.*
       ,'excuses' as type
from excuses e
inner join years y 
ON (e.date BETWEEN y.from_date AND y.to_date)
where y.name=? AND e.student_id =?
")]
    (jdbc/query
     pgdb
     [q year-name student-id]))
  )

(defn get-student-list-in-out []
  (let [q (str "
select stu.name
       , stu._id
        , CASE WHEN l.outs > l.ins=true THEN 'out'
            ELSE 'in'
          END as last_swipe_type
        , CASE WHEN l.outs > l.ins=true THEN l.outs
            ELSE l.ins
          END as last_swipe_date
from students stu
inner join 
(select 
         max(s.in_time) as ins
        , max(s.out_time) as outs
        , s.student_id
from swipes s
group by s.student_id
order by ins, outs) as l on (l.student_id = stu._id)
")]
    (jdbc/query
     pgdb
     [q ]))
  )
;; (map :student_id (get-student-list-in-out))

(defn get-report [year-name]
  (let [q (str "
select
     stu.student_id
     , sum(CASE WHEN stu.interval >= 300 THEN 1 ELSE 0 END) as good
     , sum(CASE WHEN stu.interval >= 300 THEN 0 ELSE 1 END) as short
from 
(select 
        student_id
        , sum(extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60) as interval
from swipes s
inner join years y 
ON ((s.out_time BETWEEN y.from_date AND y.to_date)
    OR (s.in_time BETWEEN y.from_date AND y.to_date))
where y.name=? 
group by student_id, to_char(s.in_time, 'YYYY-MM-DD')) as stu
group by stu.student_id
")]
    (jdbc/query
     pgdb
     [q year-name]))
  )
;; (get-report "2014-06-01 2015-06-01" )
;; (def t (get-report "2014-06-01 2015-06-01" ))
;; (count t)

(defn get-swipes-in-year [year-name student-id]
  (let [q (str "
select s.*
       ,'swipes' as type
       , extract(EPOCH FROM (s.out_time - s.in_time)::INTERVAL)/60 as interval
       , to_char(s.in_time at time zone 'America/New_York', 'HH:MI:SS') as nice_in_time
       , to_char(s.out_time at time zone 'America/New_York', 'HH:MI:SS') as nice_out_time
from swipes s
inner join years y 
ON ((s.out_time BETWEEN y.from_date AND y.to_date)
    OR (s.in_time BETWEEN y.from_date AND y.to_date))
where y.name=? AND s.student_id =? 
")]
    (jdbc/query
     pgdb
     [q year-name student-id]))
  )

;; (get-swipes-in-year "2014-06-01 2015-06-01" 1)

(defn get-school-days [year-name]
  (let [q (str "
select distinct days2.days
from (select
       to_char(s.in_time at time zone 'America/New_York', 'YYYY-MM-DD') as days
       , s.in_time
from swipes s
inner join years y 
  ON ((s.out_time BETWEEN y.from_date AND y.to_date)
      OR (s.in_time BETWEEN y.from_date AND y.to_date))
where y.name=?) days2
order by days2.days
")]
    (jdbc/query
     pgdb
     [q year-name]))
  )

;; (map :days (get-school-days "2014-06-01 2015-06-01"))

(defn get-school-days-aflj [id]
  (jdbc/query pgdb ["select * from students"])
  (jdbc/query pgdb ["select * from swipes"])
  (jdbc/query pgdb ["select * from years"])
  
  
  )

(defn get-*
  ([type] (map #(assoc % :type (keyword type))
               (jdbc/query pgdb [(str "select * from " type " order by inserted_date ")])))
  ([type id id-col]
     (map #(assoc % :type (keyword type))
          (if id
            (jdbc/query pgdb [(str "select * from " type
                                   " where " id-col "=?"
                                   " order by inserted_date")
                              id])
            (jdbc/query pgdb [(str "select * from " type " order by inserted_date")])))))

