(ns overseer.database
  (:require [com.ashafa.clutch :as couch]
            [overseer.db :as db]
            [overseer.helpers :refer :all]
            [overseer.dates :refer :all]
            [clojure.tools.trace :as trace]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            ))

(defn get-swipes
  ([] (db/get-* "swipes"))
  ([id]
     (db/get-* "swipes" id "student_id")))

(defn get-overrides [id]
  (db/get-* "overrides" id "student_id"))

(defn get-excuses [id]
  (db/get-* "excuses" id "student_id"))

(trace/deftrace lookup-last-swipe-for-day [id day]
  (let [last (db/lookup-last-swipe id)]
    (when (= day (make-date-string (:in_time last)))
      last)))

(defn only-swiped-in? [in-swipe] (and in-swipe (not (:out_time in-swipe))))

(declare swipe-out)

(defn make-swipe [student-id]
  {:type :swipes :student_id student-id :in_time nil :out_time nil})

(defn delete-swipe [swipe]
  (db/delete! swipe))

(defn make-timestamp [t]
  (->> t str (f/parse) c/to-timestamp))

;; (make-sqldate "2015-03-30")
(defn- make-sqldate [t]
  (->> t str (f/parse) c/to-sql-date))

(trace/deftrace swipe-in
  ([id] (swipe-in id (t/now)))
  ([id in-time]
     (db/persist! (assoc (make-swipe id) :in_time (make-timestamp in-time)))))

(defn sanitize-out [swipe]
  (let [in (:in_time swipe)
         in (when in (c/from-sql-time in))
        out (:out_time swipe)
        out (when out (c/from-sql-time out))
             ]
    (if (and in out)
      (if (or (not (t/before? in out))
              (not (= (t/day in) (t/day out))))
        (assoc swipe :out_time (:in_time swipe))
        swipe)
      swipe)))


(def nine-am (t/today-at 13 0))
(def four-pm (t/today-at 20 0))

(defn round-swipe-in-time [time]
  (if (t/after? time nine-am) time nine-am))

(defn round-swipe-out-time [time]
  (if (t/before? time four-pm) time four-pm))

;; (sample-db)
(trace/deftrace swipe-out
  ([id] (swipe-out id (t/now)))
  ([id out-time]
     (let [last-swipe (lookup-last-swipe-for-day id (make-date-string out-time))
           only-swiped-in? (only-swiped-in? last-swipe)
           in-swipe (if only-swiped-in?
                      last-swipe
                      (make-swipe id))
           out-swipe (assoc in-swipe :out_time (make-timestamp out-time))
           out-swipe (sanitize-out out-swipe)]
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

(trace/deftrace delete-year [year]
  (when-let [year (first (get-years year))]
    (db/delete! year)))

(trace/deftrace rename [_id name]
  (db/update! :students _id {:name name}))

(trace/deftrace excuse-date [id date-string]
  (db/persist! {:type :excuses
                :student_id id
                :date (make-sqldate date-string)}))

(trace/deftrace override-date [id date-string]
  (db/persist! {:type :overrides
                :student_id id
                :date (make-sqldate date-string)}))

;; (get-students )
(defn get-students
  ([] (db/get-* "students"))
  ([id] (db/get-* "students" id "_id")))

;; (get-years)
(defn student-not-yet-created [name]
  (empty? (filter #(= name (:name %)) (get-students))))

(trace/deftrace make-student [name]
  (when (student-not-yet-created name)
    (db/persist! {:type :students :name name :olderdate nil :show_as_absent nil})))

(defn- toggle-date [older]
  (if older nil (make-sqldate (str (t/now)))))

(trace/deftrace toggle-student-older [_id]
  (let [student (first (get-students _id))
        student (assoc student :olderdate (toggle-date (:olderdate student)))]
    (db/update! :students _id {:olderdate (:olderdate student)})
    student))

(trace/deftrace toggle-student-absent [_id]
  (let [student (first (get-students _id))
        student (assoc student :show_as_absent (make-sqldate (str (t/now))))]
    (db/update! :students _id {:show_as_absent (:show_as_absent student)})
    student))

(trace/deftrace make-year [from to]
  (let [from (f/parse from)
        to (f/parse to)
        name (str (f/unparse date-format from) " "  (f/unparse date-format to))]
    (->> {:type :years
          :from_date (make-timestamp from)
          :to_date (make-timestamp to)
          :name name}
         db/persist!)))

;; (sample-db true)
(defn sample-db
  ([] (sample-db false))
  ([have-extra?]
     (db/init-pg)
     (db/reset-db)
     (make-year (str (t/date-time 2014 6)) (str (t/plus (t/now) (t/days 2))))
     (make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
     (let [s (make-student "jim")]
       (when have-extra? (swipe-in (:_id s) (t/minus (t/now) (t/days 2)))))
     (let [s (make-student "steve")]
       (when have-extra? (swipe-in (:_id s) (t/minus (t/now) (t/days 1) (t/hours 5))))))
  )

;; (huge-sample-db)
(defn huge-sample-db []
  (db/init-pg)
  (db/reset-db)
  (make-year (str (t/date-time 2014 6)) (str (t/plus (t/now) (t/days 1))))
  (make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
  (loop [x 1]
    (if (> x 80)
      :done
      (do (let [s (make-student (str "zax" x))]
            (loop [y 2]
              (trace/trace (str "Id:" x " Num:" y " of:" (* 80 200)))
              (if (> y 200)
                :done
                (do
                  (swipe-in x (t/minus (t/now) (t/days y)))
                  (swipe-out x (t/minus (t/plus (t/now) (t/minutes 5))
                                        (t/days y)))
                  (recur (inc y))))))

          (recur (inc x)))))
  )
