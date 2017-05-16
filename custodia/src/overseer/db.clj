(ns overseer.db
  (:import [java.sql PreparedStatement]
           [java.util Date Calendar TimeZone])
  (:require [clj-time.coerce :as timec]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [overseer.database.connection :refer [pgdb init-pg]]

            [overseer.queries.demo :as demo]
            [overseer.queries.phillyfreeschool :as pfs]
            ))

(def ^:dynamic *school-id* 1)

(extend-type Date
  jdbc/ISQLParameter
  (set-parameter [val ^PreparedStatement stmt ix]
    (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
      (.setTimestamp stmt ix (timec/to-timestamp val) cal))))


(defmacro q [n args]
  `(let [s# *school-schema*
         con# @pgdb]
     (let [f# (resolve (symbol (str "overseer.queries." s# "/" ~(name n))))]
       (log/info f# ~args)
       (f# ~args {:connection con#}))))

(defn append-schema [q]
  (str "overseer." q))

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
  ([type]
   (map #(assoc % :type (keyword type))
        (jdbc/query @pgdb [(str "select * from "
                                (append-schema (name type))
                                " order by inserted_date ")
                           ])))
  ([type id id-col]
   (map #(assoc % :type (keyword type))
        (if id
          (jdbc/query @pgdb [(str "select * from " (append-schema (name type))
                                  " where " id-col "=?"
                                  " order by inserted_date")
                             id ])
          (get-* type)))))

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
  (q get-classes-and-students-y {:class_id class-id :school_id *school-id*} ))

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
  (q student-list-in-out-y {:show_archived show-archived :school_id *school-id*} ))
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
  (q swipes-in-year-y {:year_name year-name :student_id student-id :school_id *school-id*} ))

;; (get-swipes-in-year "2014-06-01 2015-06-01" 1)
