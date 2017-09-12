(ns overseer.commands
  (:require [overseer.db :as db]
            [overseer.database.users :as users]
            [overseer.queries :as queries]
            [overseer.roles :as roles]
            [yesql.core :refer [defqueries]]
            [overseer.dates :as dates]
            [clojure.tools.logging :as log]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.set :as set]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [schema.core :as s]
            [environ.core :refer [env]]
            ))

(defqueries "overseer/yesql/commands.sql" )

(defn activate-class
  ([id] (activate-class id db/*school-id*))
  ([id school-id]
    (db/q activate-class-y! {:id id :school_id school-id})))

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

(defn delete-student [id]
  (db/delete! {:type "students" :_id id}))

(defn get-name-from-dst-fields [student school]
  (if (:use_display_name school)
    (if (> (count (:display_name student)) 0)
      (:display_name student)
      (:first_name student))
    (str (:first_name student) " " (:last_name student))))

(defn ensure-correct-student-name [student school]
  (let [dst-name (get-name-from-dst-fields student school)]
    (if (not (= (:name student) dst-name))
      (db/update! :students (:_id student) {:name dst-name}))))

(defn edit-student
  ([_id name start-date email] (edit-student _id name start-date email false))
  ([_id name start-date email is_teacher]
   (db/update! :students _id
               {:name name
                :start_date (make-sqldate start-date)
                :guardian_email email
                :is_teacher is_teacher})))

(defn excuse-date [id date-string]
  (db/persist! {:type :excuses
                :student_id id
                :date (make-sqldate date-string)}))

(defn override-date [id date-string]
  (db/persist! {:type :overrides
                :student_id id
                :date (make-sqldate date-string)}))

(defn make-class
  ([name] (make-class name nil nil))
  ([name from_date to_date]
    (make-class name from_date to_date db/*school-id*))
  ([name from_date to_date school_id]
   (let [active (-> (queries/get-classes school_id) seq boolean not)]
     (when (queries/class-not-yet-created name school_id)
       (db/persist! {:type :classes :name name :active active :school_id school_id})))))

(defn add-student-to-class [student-id class-id]
  (db/persist! {:type :classes_X_students :student_id student-id :class_id class-id}))

(defn remove-student-from-class [student-id class-id]
  (db/delete-where!
    :classes_X_students
    ["student_id=? AND class_id=?" student-id class-id]))

(defn- has-name [n]
  (not= "" (clojure.string/trim n)))

(defn make-student
  ([name] (make-student name nil))
  ([name start-date] (make-student name start-date ""))
  ([name start-date email] (make-student name start-date email false))
  ([name start-date email is_teacher]
   (when (and (has-name name)
              (queries/student-not-yet-created name))
     (db/persist! {:type :students
                   :name name
                   :school_id db/*school-id*
                   :start_date (make-sqldate start-date)
                   :guardian_email email
                   :olderdate nil
                   :is_teacher is_teacher
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

(defn toggle-student-teacher [_id]
  (modify-student _id :is_teacher #(not (:is_teacher %))))

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

(defn get-start-of-year []
  (let [now (t/now)
        year-adjustment (if (< (t/month now) 8) -1 0)]
    (t/date-time (+ year-adjustment (t/year now)) 8 1)))

(defn update-schools-from-dst []
  (let [schools (queries/get-schools-with-dst)
        new-schools (filter #(= (:_id %) nil) schools)]
    (run! (fn [school]
            (db/persist! {:type :schools :name (:name school) :_id (:id school)}))
          new-schools)
    (run! (fn [school]
            (users/make-user (str (:short_name school) "-admin") (env :adminpass) #{roles/admin roles/user} (:id school))
            (users/make-user (str (:short_name school)) (env :userpass) #{roles/user} (:id school)))
          schools)))

(s/defn delete-removed-students [students-to-keep :- [s/Num]]
  (db/q delete-inactive-students-y! {:students_to_keep students-to-keep}))

(s/defn bulk-update-student-names [students :- [{:_id s/Num :name s/Str}] school]
  (run! #(ensure-correct-student-name % school) students))

(def DstStudent {:person_id s/Num
                 :first_name s/Str
                 :last_name s/Str})

(s/defn create-dst-student [name :- s/Str dst-id :- s/Num school-id :- s/Num]
  (let [new-el {:type :students
                :name name
                :school_id school-id
                :dst_id dst-id
                :start_date nil
                :guardian_email nil
                :olderdate nil
                :is_teacher false
                :show_as_absent nil}]
    (db/persist! new-el)))

(s/defn bulk-insert-new-students [dst-students :- [DstStudent] class-id :- s/Num school]
  (run! (fn [dst]
          (let [name (get-name-from-dst-fields dst school)
                dst-id (:person_id dst)
                {student-id :_id} (create-dst-student name dst-id (:_id school))]
            (add-student-to-class student-id class-id)))
        dst-students))

(s/defn ensure-existing-students-in-class
  [student-ids :- [s/Num] class-id :- s/Num school-id :- s/Num]
  (let [students-in-class (->> (queries/get-all-classes-and-students school-id)
                               :classes
                               (filter #(= (:_id %) class-id))
                               first
                               :students
                               (map :student_id)
                               set)
        students-not-in-class (set/difference (set student-ids) students-in-class)
        students-to-remove (set/difference students-in-class (set student-ids))]
    (run! (fn [student-id]
            (add-student-to-class student-id class-id))
          students-not-in-class)
    (run! (fn [student-id]
          (remove-student-from-class student-id class-id))
        students-to-remove)))

(defn update-all-students-from-dst [school]
  (let [school-id (:_id school)
        students (queries/get-students-with-dst school-id)
        new-students (filter #(= nil (:dst_id %)) students)
        existing-students (filter #(not= nil (:dst_id %)) students)
        start-of-year (get-start-of-year)
        class-name (str (t/year start-of-year) "-" (+ 1 (t/year start-of-year)))
        end-of-year (t/date-time (+ 1 (t/year start-of-year)) 7 31)
        _ (make-class class-name start-of-year end-of-year school-id)
        {class-id :_id} (queries/get-class-by-name class-name school-id)]
    (activate-class class-id school-id)

    ; Create student records for people who don't exist
    ; and add them to a class
    (bulk-insert-new-students new-students class-id school)
    (ensure-existing-students-in-class (map :_id existing-students) class-id school-id)
    (bulk-update-student-names existing-students school)
    ))

(defn update-from-dst []
  (update-schools-from-dst)
  (run! (fn [school]
          (update-all-students-from-dst school))
        (queries/get-schools-with-dst)))
