(ns overseer.commands
  (:require [overseer.db :as db]
            [overseer.queries :as queries]
            [yesql.core :refer [defqueries]]
            [overseer.dates :as dates]
            [clojure.tools.logging :as log]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [schema.core :as s]))

(defqueries "overseer/yesql/commands.sql" )

(defn activate-class [id]
  (db/q activate-class-y! {:id id :school_id db/*school-id*} ))

(defn delete-student-from-class [student-id class-id]
  (db/q delete-student-from-class-y! {:student_id student-id :class_id class-id} ))

(defn- only-swiped-in? [in-swipe]
  (and in-swipe (not (:out_time in-swipe))))

(declare swipe-out)

(defn make-swipe [student-id]
  {:type :swipes :student_id student-id :in_time nil :out_time nil :swipe_day nil})

(defn delete-swipe [swipe]
  (db/delete! swipe))

(s/defn make-timestamp :- java.sql.Timestamp
  [t :- dates/DateTime] (c/to-timestamp t))

;; (make-sqldate "2015-03-30")
(defn- make-sqldate [t]
  (->> t str (f/parse) c/to-sql-date))

(defn swipe-in
  ([id] (swipe-in id (t/now)))
  ([id in-time]
   (let [in-timestamp (make-timestamp in-time)
         rounded-in-timestamp (make-timestamp (dates/round-swipe-time in-time))]
     (db/persist! (assoc (make-swipe id)
                         :swipe_day (c/to-sql-date rounded-in-timestamp)
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

(defn swipe-out
  ([id] (swipe-out id (t/now)))
  ([id out-time]
   (let [rounded-out-time (dates/round-swipe-time out-time)
         out-time (dates/cond-parse-date-string out-time)
         last-swipe (log/spyf "Last Swipe: %s" (queries/lookup-last-swipe-for-day id (dates/make-date-string rounded-out-time)))
         only-swiped-in? (only-swiped-in? last-swipe)
         in-swipe (if only-swiped-in?
                    last-swipe
                    (make-swipe id))
         rounded-out-timestamp (make-timestamp rounded-out-time)
         out-swipe (assoc in-swipe
                          :out_time (make-timestamp out-time)
                          :rounded_out_time rounded-out-timestamp)
         out-swipe (sanitize-out out-swipe)
         interval (dates/calculate-interval out-swipe)
         out-swipe (assoc out-swipe
                          :intervalmin interval
                          :swipe_day (c/to-sql-date rounded-out-timestamp))]
     (if only-swiped-in?
       (db/update! :swipes (:_id out-swipe) out-swipe)
       (db/persist! out-swipe))
     out-swipe)))

(defn delete-year [year]
  (when-let [year (first (queries/get-years year))]
    (db/delete! year)))

(defn edit-student [_id name start-date email]
  (db/update! :students _id
              {:name name
               :start_date (make-sqldate start-date)
               :guardian_email email}))

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

(defn make-class
  ([name] (make-class name nil nil))
  ([name from_date to_date]
   (let [active (-> (queries/get-classes) seq boolean not)]
     (when (queries/class-not-yet-created name)
       (db/persist! {:type :classes :name name :active active :school_id db/*school-id*})))))

(defn add-student-to-class [student-id class-id]
  (db/persist! {:type :classes_X_students :student_id student-id :class_id class-id}))

(defn- has-name [n]
  (not= "" (clojure.string/trim n)))

(defn make-student
  ([name] (make-student name nil))
  ([name start-date] (make-student name start-date ""))
  ([name start-date email]
   (when (and (has-name name)
              (queries/student-not-yet-created name))
     (db/persist! {:type :students
                   :name name
                   :school_id db/*school-id*
                   :start_date (make-sqldate start-date)
                   :guardian_email email
                   :olderdate nil
                   :show_as_absent nil}))))

(defn make-student-starting-today
  ([name] (make-student-starting-today name ""))
  ([name email] (make-student name (dates/today-string) email)))

(defn- toggle-date [older]
  (if older nil (make-sqldate (str (t/now)))))

(defn modify-student [_id field f]
  (let [student (first (queries/get-students _id))
        newVal (f student)]
    (db/update! :students _id {field newVal})
    (assoc student field newVal)))

(defn toggle-student-older [_id]
  (modify-student _id :olderdate #(toggle-date (:olderdate %))))

(defn set-student-start-date [_id date]
  (modify-student _id  :start_date (fn [_] (make-sqldate date))))

(defn toggle-student-absent [_id]
  (modify-student _id :show_as_absent (fn [_] (make-sqldate (str (t/now))))))

(defn make-year [from to]
  (let [from (f/parse from)
        to (f/parse to)
        name (str (f/unparse dates/date-format from) " "  (f/unparse dates/date-format to))]
    (->> {:type :years
          :from_date (make-timestamp from)
          :to_date (make-timestamp to)
          :school_id db/*school-id*
          :name name}
         db/persist!)))
