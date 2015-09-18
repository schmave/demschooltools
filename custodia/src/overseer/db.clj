(ns overseer.db
  (:import
   [java.util Date Calendar TimeZone]
   [java.sql PreparedStatement])
  (:require [carica.core :as c]
            [yesql.core :refer [defqueries]]
            [cemerick.url :as url]
            [clojure.tools.trace :as trace]
            [heroku-database-url-to-jdbc.core :as h]
            [migratus.core :as migratus]
            [com.ashafa.clutch :as couch]
            [environ.core :refer [env]]
            [clj-time.coerce :as timec]
            [clj-time.format :as timef]
            [clojure.java.jdbc :as jdbc]
            [overseer.migrations :as migrations]
            ))

(defqueries "overseer/queries.sql")

(extend-type Date
  jdbc/ISQLParameter
  (set-parameter [val ^PreparedStatement stmt ix]
    (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
      (.setTimestamp stmt ix (timec/to-timestamp val) cal))))

(def pgdb (atom nil))
;; (init-pg)
(defn init-pg []
  (swap! pgdb (fn [old]
                (dissoc (h/korma-connection-map (env :database-url))
                        :classname))))

(defn create-all-tables []
  (jdbc/execute! @pgdb [migrations/initialize-prod-database]))

(defn drop-all-tables []
  (jdbc/execute! @pgdb ["
    DROP TABLE IF EXISTS years;
    DROP TABLE IF EXISTS classes_X_students;
    DROP TABLE IF EXISTS classes;
    DROP FUNCTION IF EXISTS school_days(text);
    DROP VIEW IF EXISTS roundedswipes;
    DROP TABLE IF EXISTS swipes;
    DROP TABLE IF EXISTS session_store;
    DROP TABLE IF EXISTS students;
    DROP TABLE IF EXISTS excuses;
    DROP TABLE IF EXISTS overrides;
"]))

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

(defn get-active-class []
  (-> @pgdb
      get-active-class-y
      first
      :_id))


;; (get-classes)
(defn get-classes []
  (let [classes (get-classes-y @pgdb)
        grouped (vals (group-by :name classes))]
    (map (fn [class-group]
           (let [base (dissoc (first class-group) :student_id :student_name)
                 ids (filter (comp not nil?) (map :student_id class-group))
                 names (filter (comp not nil?) (map :student_name class-group))
                 students (map (fn [id name] {:student_id id :name name}) ids names)]
             (assoc base :students students)))
         grouped)))

(defn get-all-classes-and-students []
  {:classes (get-classes)
   :students (map (fn [s] {:name (:name s) :_id (:_id s)})
                  (get-* "students"))}
  )
;; (get-all-classes-and-students)

(defn activate-class [id]
  (activate-class-y! @pgdb id))

(defn delete-student-from-class [student-id class-id]
  (delete-student-from-class-y! @pgdb student-id class-id))

(defn get-students-for-class [class-id]
  (get-classes-and-students-y @pgdb class-id))

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

(defn get-swipes-in-year [year-name student-id]
  (swipes-in-year-y @pgdb year-name student-id))

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

