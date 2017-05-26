(ns overseer.db
  (:import [java.sql PreparedStatement]
           [java.util Date Calendar TimeZone])
  (:require [clj-time.coerce :as timec]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defqueries] ]
            [clojure.tools.logging :as log]
            [overseer.database.connection :refer [pgdb init-pg]]
            [clojure.tools.trace :as trace]))

(defqueries "overseer/school_queries.sql" )

(def ^:dynamic *school-id* 1)

(extend-type Date
  jdbc/ISQLParameter
  (set-parameter [val ^PreparedStatement stmt ix]
    (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
      (.setTimestamp stmt ix (timec/to-timestamp val) cal))))

(defn q [n args]
  (n args {:connection @pgdb}))

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
  ([type id id-col]
   (map #(assoc % :type (keyword type))
        (jdbc/query @pgdb [(str "select * from " (append-schema (name type))
                                " where " id-col "=?"
                                " order by inserted_date")
                           id ]))))

(defn get-active-class []
  (-> (q get-active-class-y {:school_id *school-id*} )
      first
      :_id))

;; (get-classes)
(defn get-classes []
  (let [classes (q get-classes-y {:school_id *school-id*} )
        grouped (vals (group-by :name classes))]
    (map (fn [class-group]
           (let [base (dissoc (first class-group) :student_id :student_name)
                 ids (filter (comp not nil?) (map :student_id class-group))
                 names (filter (comp not nil?) (map :student_name class-group))
                 students (map (fn [id name] {:student_id id :name name}) ids names)]
             (assoc base :students students)))
         grouped)))

(defn get-all-years []
  (q get-years-y {:school_id *school-id*}))

(defn get-all-students []
  (q get-students-y {:school_id *school-id*}))

(defn get-all-classes-and-students []
  {:classes (get-classes)
   :students (map (fn [s] {:name (:name s) :_id (:_id s)})
                  (get-all-students))})

(defn activate-class [id]
  (q activate-class-y! {:id id :school_id *school-id*} ))

(defn delete-student-from-class [student-id class-id]
  (q delete-student-from-class-y! {:student_id student-id :class_id class-id} ))

(defn get-students-for-class [class-id]
  (q get-classes-and-students-y {:class_id class-id :school_id *school-id*} ))

(defn get-overrides-in-year [year-name student-id]
  (q get-overrides-in-year-y {:year_name year-name :student_id student-id} ))

(defn lookup-last-swipe [student-id]
  (first (q lookup-last-swipe-y {:student_id student-id} )))

(defn get-excuses-in-year [year-name student-id]
  (q get-excuses-in-year-y {:year_name year-name :student_id student-id} ))

(defn get-student-list-in-out [show-archived]
  (q student-list-in-out-y {:show_archived show-archived :school_id *school-id*} ))

(defn get-schools
  ([] (q get-schools-y {}))
  ([id] (first (filter (fn [s] (= id (:_id s)))
                       (q get-schools-y {})))))

(defn get-current-school []
  (get-schools *school-id*))

(defn get-school-time-zone []
  (:timezone (get-current-school)))

(defn get-school-days [year-name]
  (q get-school-days-y {:year_name year-name :school_id *school-id* :timezone (get-school-time-zone)} ))


(defn get-student-page
  ([student-id year] (get-student-page student-id year (get-active-class)))
  ([student-id year class-id]
   (q get-student-page-y {:year_name year :student_id student-id :class_id class-id :timezone (get-school-time-zone)} )))

(defn get-report
  ([year-name] (get-report year-name (get-active-class)))
  ([year-name class-id]
   (q student-report-y { :year_name year-name :class_id class-id :timezone (get-school-time-zone)} )))

(defn get-swipes-in-year [year-name student-id]
  (q swipes-in-year-y {:year_name year-name :student_id student-id :school_id *school-id*  :timezone (get-school-time-zone)} ))

;; (get-students )
(defn get-students
  ([] (sort-by :name (get-all-students)))
  ([id] (get-* "students" id "_id")))
