(ns overseer.queries
  (:import [java.sql PreparedStatement]
           [java.util Date Calendar TimeZone])
  (:require [yesql.core :refer [defqueries] ]
            [overseer.dates :as dates]
            [overseer.db :as db]))

(defqueries "overseer/yesql/queries.sql" )


(defn get-active-class []
  (-> (db/q get-active-class-y {:school_id db/*school-id*} )
      first
      :_id))

;; (get-classes)
(defn get-classes
  ([] (get-classes db/*school-id*))
  ([school_id]
    (let [classes (db/q get-classes-y {:school_id school_id} )
          grouped (vals (group-by :name classes))]
      (map (fn [class-group]
             (let [base (dissoc (first class-group) :student_id :student_name)
                   ids (filter (comp not nil?) (map :student_id class-group))
                   names (filter (comp not nil?) (map :student_name class-group))
                   students (map (fn [id name] {:student_id id :name name}) ids names)]
               (assoc base :students students)))
           grouped))))

(defn get-all-years []
  (db/q get-years-y {:school_id db/*school-id*}))

;; TODO - make multimethod on type
;; (get-years)
(defn get-years
  ([] (get-all-years))
  ([names]
   (filter (fn [y] (= names {:name y})) (get-all-years))))

(defn get-all-students []
  (db/q get-students-y {:school_id db/*school-id*}))

(defn get-student [student_id]
  (db/q get-student-y {:student_id student_id
                       :school_id db/*school-id*}))

(defn get-all-classes-and-students []
  {:classes (get-classes)
   :students (map (fn [s] {:name (:name s) :_id (:_id s)})
                  (get-all-students))})

(defn get-students-for-class [class-id]
  (db/q get-classes-and-students-y {:class_id class-id :school_id db/*school-id*} ))

(defn get-overrides-in-year [year-name student-id]
  (db/q get-overrides-in-year-y {:year_name year-name :student_id student-id} ))

(defn lookup-last-swipe [student-id]
  (first (db/q lookup-last-swipe-y {:student_id student-id} )))

(defn get-excuses-in-year [year-name student-id]
  (db/q get-excuses-in-year-y {:year_name year-name :student_id student-id} ))

(defn get-student-list-in-out [show-archived]
  (db/q student-list-in-out-y {:show_archived show-archived :school_id db/*school-id*} ))

(defn get-schools
  ([] (db/q get-schools-y {}))
  ([id] (first (filter (fn [s] (= id (:_id s)))
                       (db/q get-schools-y {})))))

(defn get-current-school []
  (get-schools db/*school-id*))

(defn get-school-time-zone []
  db/*school-timezone*)

(defn get-school-days [year-name]
  (db/q get-school-days-y {:year_name year-name :school_id db/*school-id* :timezone (get-school-time-zone)} ))


(defn get-student-page
  ([student-id year] (get-student-page student-id year (get-active-class)))
  ([student-id year class-id]
   (db/q get-student-page-y {:year_name year :student_id student-id :class_id class-id :timezone (get-school-time-zone)} )))

(defn get-report
  ([year-name] (get-report year-name (get-active-class)))
  ([year-name class-id]
   (db/q student-report-y { :year_name year-name :class_id class-id :timezone (get-school-time-zone)} )))

(defn get-swipes-in-year [year-name student-id]
  (db/q swipes-in-year-y {:year_name year-name :student_id student-id :school_id db/*school-id*  :timezone (get-school-time-zone)} ))

;; (get-students )
(defn get-students
  ([] (sort-by :name (get-all-students)))
  ([id] (get-student id)))

(defn get-class-by-name
  ([name] (get-class-by-name name db/*school-id*))
  ([name school_id] (first (db/q get-class-y {:name name :school_id school_id}))))

(defn- thing-not-yet-created [name getter]
  (empty? (filter #(= name (:name %)) (getter))))

(defn student-not-yet-created [name]
  (thing-not-yet-created name get-students))

(defn class-not-yet-created [name school-id]
  (thing-not-yet-created name #(get-classes school-id)))

(defn lookup-last-swipe-for-day [id day]
  (let [last (lookup-last-swipe id)]
    (when (= day (dates/make-date-string (:in_time last)))
      last)))

(defn get-students-with-dst [school-id]
  (db/q get-students-with-dst-y {:school_id school-id}))

(defn get-schools-with-dst []
  (db/q get-schools-with-dst-y {}))


