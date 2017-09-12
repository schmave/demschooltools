(ns overseer.db
  (:import [java.sql PreparedStatement]
           [java.util Date Calendar TimeZone])
  (:require [clj-time.coerce :as timec]
            [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [overseer.database.connection :refer [pgdb init-pg]]))

(def ^:dynamic *school-id* 1)
;; defaulted for unit tests
(def ^:dynamic *school-timezone* "America/New_York")

(extend-type Date
  jdbc/ISQLParameter
  (set-parameter [val ^PreparedStatement stmt ix]
    (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
      (.setTimestamp stmt ix (timec/to-timestamp val) cal))))

(defn q [n args]
  (n args {:connection @pgdb}))

(defn- append-schema [q]
  (str "overseer." q))

(defn delete! [doc]
  (let [table (append-schema (name (:type doc)))
        id (:_id doc)]
    (jdbc/delete! @pgdb table ["_id=?" id])))

(defn delete-where! [table where-clause]
  (let [table (append-schema (name table))]
    (jdbc/delete! @pgdb table where-clause)))

(defn update!
  ([table id fields]
   (update! table id fields "_id"))
  ([table id fields where-id]
   (let [fields (dissoc fields :type)
         table (append-schema (name table))]
     (jdbc/update! @pgdb table fields [(str where-id "=?") id]))))

(defn persist! [doc]
  (let [table (:type doc)
        tablename (append-schema (name (:type doc)))
        doc (dissoc doc :type)]
    (first (map #(assoc % :type table)
                (jdbc/insert! @pgdb tablename doc)))))

