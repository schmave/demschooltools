(ns overseer.database
  (:require [overseer.db :as db]
            [overseer.helpers :refer :all]
            [overseer.dates :refer :all]
            [clojure.tools.trace :as trace]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [schema.core :as s]
            ))

(defn get-swipes
  ([] (db/get-* "swipes"))
  ([id]
   (db/get-* "swipes" id "student_id")))

(defn get-overrides [id]
  (db/get-* "overrides" id "student_id"))

(defn get-excuses [id]
  (db/get-* "excuses" id "student_id"))

(defn lookup-last-swipe-for-day [id day]
  (let [last (db/lookup-last-swipe id)]
    (when (= day (make-date-string (:in_time last)))
      last)))

(defn only-swiped-in? [in-swipe] (and in-swipe (not (:out_time in-swipe))))

(declare swipe-out)

(defn make-swipe [student-id]
  {:type :swipes :student_id student-id :in_time nil :out_time nil})

(defn delete-swipe [swipe]
  (db/delete! swipe))

(s/defn make-timestamp :- java.sql.Timestamp
  [t :- DateTime] (c/to-timestamp t))

;; (make-sqldate "2015-03-30")
(defn- make-sqldate [t]
  (->> t str (f/parse) c/to-sql-date))

(defn swipe-in
  ([id] (swipe-in id (t/now)))
  ([id in-time]
   (let [in-timestamp (make-timestamp in-time)
         rounded-in-timestamp (make-timestamp (round-swipe-time in-time))
         ]
     (db/persist! (assoc (make-swipe id)
                         :rounded_in_time rounded-in-timestamp
                         :in_time in-timestamp)))))

(defn sanitize-out [swipe]
  (let [in (:in_time swipe)
        in (when in (c/from-sql-time in))
        out (:out_time swipe)
        out (when out (c/from-sql-time out))]
    (if (and (and in out)
             (or (not (t/before? in out))
                 (not (= (t/day in) (t/day out)))))
      (assoc swipe
             :out_time (:in_time swipe)
             :rounded_out_time (:rounded_in_time swipe))
      swipe)))

;; (sample-db)
(defn swipe-out
  ([id] (swipe-out id (t/now)))
  ([id out-time]
   (let [rounded-out-time (round-swipe-time out-time)
         out-time (cond-parse-date-string out-time)
         last-swipe (trace/trace "Last Swipe" (lookup-last-swipe-for-day id (make-date-string rounded-out-time)))
         only-swiped-in? (only-swiped-in? last-swipe)
         in-swipe (if only-swiped-in?
                    last-swipe
                    (make-swipe id))
         out-swipe (assoc in-swipe
                          :out_time (make-timestamp out-time)
                          :rounded_out_time (make-timestamp rounded-out-time))
         out-swipe (sanitize-out out-swipe)
         interval (calculate-interval out-swipe)
         out-swipe (assoc out-swipe :intervalmin interval)]
     (if only-swiped-in?
       (db/update! :swipes (:_id out-swipe) out-swipe)
       (db/persist! out-swipe))
     out-swipe)))

;; TODO - make multimethod on type
;; (get-years)
(defn get-years
  ([] (db/get-* "years"))
  ([names]
   (db/get-* "years" names "name")))

(defn delete-year [year]
  (when-let [year (first (get-years year))]
    (db/delete! year)))

(defn edit-student [_id name start-date]
  (db/update! :students _id {:name name :start_date (make-sqldate start-date)}))

(defn excuse-date [id date-string]
  (db/persist! {:type :excuses
                :student_id id
                :date (make-sqldate date-string)}))

(defn override-date [id date-string]
  (db/persist! {:type :overrides
                :student_id id
                :date (make-sqldate date-string)}))

(defn insert-email [email]
  (db/persist! {:type :emails
                :email email}))

;; (get-students )
(defn get-students
  ([] (db/get-* "students"))
  ([id] (db/get-* "students" id "_id")))

(defn get-class-by-name
  ([name] (first (db/get-* "classes" name "name"))))

(defn thing-not-yet-created [name getter]
  (empty? (filter #(= name (:name %)) (getter))))

;; (get-years)
(defn student-not-yet-created [name]
  (thing-not-yet-created name get-students))

(defn class-not-yet-created [name]
  (thing-not-yet-created name db/get-classes))

(defn make-class [name]
  (when (class-not-yet-created name)
    (db/persist! {:type :classes :name name :active false})))

(defn add-student-to-class [student-id class-id]
  (db/persist! {:type :classes_X_students :student_id student-id :class_id class-id}))

(defn make-student
  ([name] (make-student name nil))
  ([name start-date]
   (when (student-not-yet-created name)
     (db/persist! {:type :students
                   :name name
                   :start_date start-date
                   :olderdate nil :show_as_absent nil}))))

(defn make-student-starting-today [name]
  (make-student name (make-sqldate (today-string))))

(defn- toggle-date [older]
  (if older nil (make-sqldate (str (t/now)))))

(defn toggle-student-older [_id]
  (let [student (first (get-students _id))
        student (assoc student :olderdate (toggle-date (:olderdate student)))]
    (db/update! :students _id {:olderdate (:olderdate student)})
    student))

(defn set-student-start-date [_id date]
  (let [student (first (get-students _id))
        student (assoc student :start_date (make-sqldate date))]
    (db/update! :students _id {:start_date (:start_date student)})
    student))

(defn toggle-student-absent [_id]
  (let [student (first (get-students _id))
        student (assoc student :show_as_absent (make-sqldate (str (t/now))))]
    (db/update! :students _id {:show_as_absent (:show_as_absent student)})
    student))

(defn make-year [from to] 
  (let [from (f/parse from)
        to (f/parse to)
        name (str (f/unparse date-format from) " "  (f/unparse date-format to))]
    (->> {:type :years
          :from_date (make-timestamp from)
          :to_date (make-timestamp to)
          :name name}
         db/persist!)))
