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

            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            ))

(def ^:dynamic *school-schema* "phillyfreeschool")

(extend-type Date
  jdbc/ISQLParameter
  (set-parameter [val ^PreparedStatement stmt ix]
    (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
      (.setTimestamp stmt ix (timec/to-timestamp val) cal))))

(def pgdb (atom nil))

(defqueries "overseer/queries.sql" )

;; (init-pg)
(defn init-pg []
  (swap! pgdb (fn [old]
                (dissoc (h/korma-connection-map (env :database-url))
                        :classname))))

(defn create-all-tables []
  (jdbc/execute! @pgdb [migrations/initialize-prod-database]))

(defn drop-all-tables []
  (jdbc/execute! @pgdb ["
    DROP TABLE IF EXISTS phillyfreeschool.users;
    DROP TABLE IF EXISTS phillyfreeschool.years;
    DROP TABLE IF EXISTS phillyfreeschool.classes_X_students;
    DROP TABLE IF EXISTS phillyfreeschool.classes;
    DROP FUNCTION IF EXISTS phillyfreeschool.school_days(text);
    DROP VIEW IF EXISTS phillyfreeschool.roundedswipes;
    DROP TABLE IF EXISTS phillyfreeschool.swipes;
    DROP TABLE IF EXISTS session_store;
    DROP TABLE IF EXISTS phillyfreeschool.students;
    DROP TABLE IF EXISTS phillyfreeschool.excuses;
    DROP TABLE IF EXISTS phillyfreeschool.overrides;
"]))

(defn reset-db []
  (drop-all-tables)
  (create-all-tables))

(defn delete! [doc]
  (let [table (str "phillyfreeschool." (name (:type doc)))
        id (:_id doc)]
    (jdbc/delete! @pgdb table ["_id=?" id])))

(defn update! [table id fields]
  (let [fields (dissoc fields :type)
        table (str "phillyfreeschool." (name table))]
    (jdbc/update! @pgdb table fields ["_id=?" id])))

(defn persist! [doc]
  (let [table (:type doc)
        tablename (str "phillyfreeschool." (name (:type doc)))
        doc (dissoc doc :type)]
    (first (map #(assoc % :type table)
                (jdbc/insert! @pgdb tablename doc)))))

(defn get-*
  ([type] (map #(assoc % :type (keyword type))
               (jdbc/query @pgdb [(str "select * from phillyfreeschool." (name type) " order by inserted_date ")])))
  ([type id id-col]
   (map #(assoc % :type (keyword type))
        (if id
          (jdbc/query @pgdb [(str "select * from phillyfreeschool." (name type)
                                  " where " id-col "=?"
                                  " order by inserted_date")
                             id])
          (jdbc/query @pgdb [(str "select * from phillyfreeschool." (name type) " order by inserted_date")])))))

(defn get-active-class []
  (-> (get-active-class-y {} {:connection @pgdb})
      first
      :_id))


;; (get-classes)
(defn get-classes []
  (let [classes (get-classes-y {} {:connection @pgdb})
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
                  (get-* "students"))})

;; (get-all-classes-and-students)

(defn activate-class [id]
  (activate-class-y! {:id id} {:connection @pgdb}))

(defn delete-student-from-class [student-id class-id]
  (delete-student-from-class-y! {:student_id student-id :class_id class-id} {:connection @pgdb}))

(defn get-students-for-class [class-id]
  (get-classes-and-students-y {:class_id class-id} {:connection @pgdb}))

;; (jdbc/query @pgdb ["select * from students"])
;; (jdbc/query @pgdb ["select * from students where id in (?)" "1"])
;; (persist! {:type :students :name "steve" :olderdate nil})
;; (update! :students 1 {:olderdate  "test"})
(defn get-overrides-in-year [year-name student-id]
  (get-overrides-in-year-y {:year_name year-name :student_id student-id} {:connection @pgdb}))

(defn lookup-last-swipe [student-id]
  (first (lookup-last-swipe-y {:student_id student-id} {:connection @pgdb})))

(defn get-excuses-in-year [year-name student-id]
  (get-excuses-in-year-y {:year_name year-name :student_id student-id} {:connection @pgdb}))

(defn get-student-list-in-out [show-archived]
  (student-list-in-out-y {:show_archived show-archived} {:connection @pgdb}))
;;(student-list-in-out-y {:show_archived true})

;; (map :student_id (get-student-list-in-out))

(defn get-school-days [year-name]
  (get-school-days-y {:year_name year-name} {:connection @pgdb}))

;; (map :days (get-school-days "2014-06-01 2015-06-01"))
;; (get-student-page 7 "2014-07-23 2015-06-17")

(defn get-student-page
  ([student-id year] (get-student-page student-id year (get-active-class)))
  ([student-id year class-id]
   (get-student-page-y {:year_name year :student_id student-id :class_id class-id} {:connection @pgdb})))

(defn get-report
  ([year-name] (get-report year-name (get-active-class)))
  ([year-name class-id]
   (student-report-y { :year_name year-name :class_id class-id} {:connection @pgdb})))

(defn get-swipes-in-year [year-name student-id]
  (swipes-in-year-y {:year_name year-name :student_id student-id} {:connection @pgdb}))

;; (get-swipes-in-year "2014-06-01 2015-06-01" 1)


(defn make-user [username school-id password roles]
  (persist! {:type "users"
             :username username
             :password (creds/hash-bcrypt password)
             :roles (str roles)}))

(defn get-user [username]
  (if-let [u (first (get-user-y { :username username} {:connection @pgdb}))]
    (assoc u :roles (read-string (:roles u)))))
