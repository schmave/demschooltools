(ns clojure-getting-started.db
  (:require [carica.core :as c]
            [cemerick.url :as url]
            [clojure.tools.trace :as trace]
            [heroku-database-url-to-jdbc.core :as h]
            [com.ashafa.clutch :as couch]
            [environ.core :refer [env]]
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
  in_time varchar(255),
  inserted_date timestamp default now(),
  out_time varchar(255)
  );")
(def create-years-table-sql
  "create table years(
  _id bigserial primary key,
  from_date varchar(255),
  to_date varchar(255),
  inserted_date timestamp default now(),
  name varchar(255)
  );")

(def create-override-table-sql
  "create table overrides(
  _id bigserial primary key,
  student_id bigserial,
  inserted_date timestamp default now(),
  date varchar(255)
  );")

(def pgdb
  (dissoc (h/korma-connection-map (env :database-url))
          :classname))

(defn create-all-tables []
  (jdbc/execute! pgdb [create-swipes-table-sql])
  (jdbc/execute! pgdb [create-override-table-sql])
  (jdbc/execute! pgdb [create-years-table-sql])
  (jdbc/execute! pgdb [create-students-table-sql]))
(defn drop-all-tables []
  (jdbc/execute! pgdb ["drop table if exists students;"])
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

