(ns overseer.db
  (:import [java.sql PreparedStatement]
           [java.util Date Calendar TimeZone])
  (:require [carica.core :as c]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])

            [cemerick.url :as url]
            [clj-time.coerce :as timec]
            [clj-time.format :as timef]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.trace :as trace]
            [com.ashafa.clutch :as couch]
            [environ.core :refer [env]]
            [migratus.core :as migratus]
            [overseer.migrations :as migrations]
            [overseer.roles :as roles]
            [overseer.queries.phillyfreeschool :as pfs]
            [overseer.queries.demo :as demo]
            [overseer.database.connection :refer [pgdb init-pg]]
            [yesql.core :refer [defqueries]]))

(def ^:dynamic *school-schema* "phillyfreeschool")

(def current-schemas ["phillyfreeschool" "demo"])

(extend-type Date
  jdbc/ISQLParameter
  (set-parameter [val ^PreparedStatement stmt ix]
    (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
      (.setTimestamp stmt ix (timec/to-timestamp val) cal))))

(defqueries "overseer/base.sql" )

(defmacro q [n args]
  `(let [s# *school-schema*
         con# @pgdb]
     (let [f# (resolve (symbol (str "overseer.queries." s# "/" ~(name n))))]
       (trace/trace f#)
       (f# ~args {:connection con#}))))

;; (get-user "admin2")
(defn get-user [username]
  (if-let [u (first (get-user-y { :username username} {:connection @pgdb}))]
    (assoc u :roles (read-string (:roles u)))))

;;(get-users)
(defn get-users []
  (->> (jdbc/query @pgdb ["select * from users;"])
      (map #(dissoc % :password))))

;;(set-user-schema "super" "TEST")
;;(get-user "super")
(defn set-user-schema [username schema]
  (jdbc/update! @pgdb :users {:schema_name schema} ["username=?" username]))

(defn make-user [username password roles]
  (if-not (get-user username)
    (jdbc/insert! @pgdb "users"
                  {:username username
                   :password (creds/hash-bcrypt password)
                   :schema_name *school-schema*
                   :roles (str roles)})))

(defn init-users []
  (make-user "admin" (env :admin) #{roles/admin roles/user} "phillyfreeschool")
  (make-user "super" (env :admin) #{roles/admin roles/user roles/super}  "phillyfreeschool")
  (make-user "user" (env :userpass) #{roles/user} "phillyfreeschool")
  (make-user "admin2" (env :admin) #{roles/admin roles/user} "demo")
  (make-user "demo" (env :userpass) #{roles/admin roles/user} "demo")
  )

(defn drop-all-tables []
  (jdbc/execute! @pgdb [(str "DROP TABLE IF EXISTS schema_migrations;"
                             "DROP TABLE IF EXISTS users; "
                             "DROP TABLE IF EXISTS session_store;"
                             "DROP SCHEMA IF EXISTS phillyfreeschool CASCADE;"
                             "DROP SCHEMA IF EXISTS demo CASCADE;")]))

;;(reset-db)
(defn reset-db []
  (drop-all-tables)
  (migrations/migrate-db @pgdb)
  (init-users))

(defn append-schema [q]
  (str *school-schema* "." q))

(defn delete! [doc]
  (let [table (append-schema (name (:type doc)))
        id (:_id doc)]
    (jdbc/delete! @pgdb table ["_id=?" id])))

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

(defn get-*
  ([type] (map #(assoc % :type (keyword type))
               (jdbc/query @pgdb [(str "select * from " (append-schema (name type)) " order by inserted_date ")])))
  ([type id id-col]
   (map #(assoc % :type (keyword type))
        (if id
          (jdbc/query @pgdb [(str "select * from " (append-schema (name type))
                                  " where " id-col "=?"
                                  " order by inserted_date")
                             id])
          (jdbc/query @pgdb [(str "select * from " (append-schema (name type)) " order by inserted_date")])))))

(defn get-active-class []
  (-> (q get-active-class-y {} )
      first
      :_id))

;; (get-classes)
(defn get-classes []
  (let [classes (q get-classes-y {} )
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
  (q activate-class-y! {:id id} ))

(defn delete-student-from-class [student-id class-id]
  (q delete-student-from-class-y! {:student_id student-id :class_id class-id} ))

(defn get-students-for-class [class-id]
  (q get-classes-and-students-y {:class_id class-id} ))

;; (jdbc/query @pgdb ["select * from students"])
;; (jdbc/query @pgdb ["select * from students where id in (?)" "1"])
;; (persist! {:type :students :name "steve" :olderdate nil})
;; (update! :students 1 {:olderdate  "test"})
(defn get-overrides-in-year [year-name student-id]
  (q get-overrides-in-year-y {:year_name year-name :student_id student-id} ))

(defn lookup-last-swipe [student-id]
  (first (q lookup-last-swipe-y {:student_id student-id} )))

(defn get-excuses-in-year [year-name student-id]
  (q get-excuses-in-year-y {:year_name year-name :student_id student-id} ))

(defn get-student-list-in-out [show-archived]
  (q student-list-in-out-y {:show_archived show-archived} ))
;;(get-student-list-in-out  true)

;; (map :student_id (get-student-list-in-out))

(defn get-school-days [year-name]
  (q get-school-days-y {:year_name year-name} ))

;; (map :days (get-school-days "2014-06-01 2015-06-01"))
;; (get-student-page 7 "2014-07-23 2015-06-17")

(defn get-student-page
  ([student-id year] (get-student-page student-id year (get-active-class)))
  ([student-id year class-id]
   (q get-student-page-y {:year_name year :student_id student-id :class_id class-id} )))

(defn get-report
  ([year-name] (get-report year-name (get-active-class)))
  ([year-name class-id]
   (q student-report-y { :year_name year-name :class_id class-id} )))

(defn get-swipes-in-year [year-name student-id]
  (q swipes-in-year-y {:year_name year-name :student_id student-id} ))

;; (get-swipes-in-year "2014-06-01 2015-06-01" 1)
