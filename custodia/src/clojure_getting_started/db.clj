(ns clojure-getting-started.db
  (:require [carica.core :as c]
            [cemerick.url :as url]
            [clojure.tools.trace :as trace]
            [com.ashafa.clutch :as couch]
            [environ.core :refer [env]]
            [clojure.java.jdbc :as jdbc]
            ))
#_(def db (assoc (url/url (env :db-url)
                          (env :db-name))
            :username (env :db-user)
            :password (env :db-password)))

(def db (cemerick.url/url "http://127.0.0.1:5984" "test"))

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
  { :subprotocol "postgresql"
   :user "postgres"
   :password "changeme"
   :subname "//localhost:5432/swipes" })

(defn create-all-tables []
  (jdbc/execute! pgdb [create-swipes-table-sql])
  (jdbc/execute! pgdb [create-override-table-sql])
  (jdbc/execute! pgdb [create-years-table-sql])
  (jdbc/execute! pgdb [create-students-table-sql]))
(defn drop-all-tables []
  (jdbc/execute! pgdb ["drop table students;"])
  (jdbc/execute! pgdb ["drop table overrides;"])
  (jdbc/execute! pgdb ["drop table years;"])
  (jdbc/execute! pgdb ["drop table swipes;"]))

(defn reset-db []
  (drop-all-tables)
  (create-all-tables))

(defn delete! [doc]
  (let [table (:type doc)
        id (:_id doc)]
    (jdbc/delete! pgdb table ["_id=?" id])))

(trace/deftrace update! [table id fields]
  (let [fields (dissoc fields :type)]
    (jdbc/update! pgdb table fields ["_id=?" id])))

(trace/deftrace persist! [doc]
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


(def design-doc
  {"_id" "_design/view"
   "views" {"students" {"map" "function(doc) {
                                 if (doc.type === \"student\") {
                                   emit(doc._id, doc);
                                 }
                               }"}
            "student-swipes" {"map"
                              "function(doc) {
                                if (doc.type == \"student\") {
                                  map([doc._id, 0], doc);
                                } else if (doc.type == \"swipe\") {
                                  map([doc.post, 1], doc);
                                }
                              }"}
            "swipes" {"map"
                      "function(doc) {
                         if (doc.type == \"swipe\") {
                           emit(doc.student_id, doc);
                         }
                       }"}
            "years" {"map"
                     "function(doc) {
                         if (doc.type == \"year\") {
                           emit(doc.name, doc);
                         }
                       }"}
            "overrides" {"map"
                         "function(doc) {
                            if (doc.type == \"override\") {
                              emit(doc.student_id, doc);
                            }
                          }"}
            }
   "language" "javascript"})

(defn make-db [] (couch/put-document db design-doc))
