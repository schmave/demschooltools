(ns clojure-getting-started.database
  (:require [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clojure-getting-started.helpers :refer :all]
            [clojure-getting-started.dates :refer :all]
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

(defn- lookup-last-swipe-for-day [id day]
  (let [last (-> (get-swipes id) last)]
    (when (= day (make-date-string (:in_time last)))
      last)))

(defn only-swiped-in? [in-swipe] (and in-swipe (not (:out_time in-swipe))))

(declare swipe-out)

(defn make-swipe [student-id]
  {:type :swipes :student_id student-id :in_time nil :out_time nil})

(defn delete-swipe [swipe]
  (db/delete! swipe))

(defn swipe-in
  ([id] (swipe-in id (t/now)))
  ([id in-time] 
     (db/persist! (assoc (make-swipe id) :in_time (str in-time)))))

(defn sanitize-out [swipe]
  (let [in (:in_time swipe)
        in (when in (f/parse in))
        out (:out_time swipe)
        out (when out (f/parse out))]
    (if (and in out)
      (if (or (not (t/before? in out))
              (not (= (t/day in) (t/day out))))
        (assoc swipe :out_time (:in_time swipe))
        swipe)
      swipe)))

;; (sample-db)   
(defn swipe-out
  ([id] (swipe-out id (t/now)))
  ([id out-time]
     (let [last-swipe (lookup-last-swipe-for-day id (make-date-string (str out-time)))
           only-swiped-in? (only-swiped-in? last-swipe)
           in-swipe (if only-swiped-in? 
                      last-swipe 
                      (make-swipe id))
           out-swipe (assoc in-swipe :out_time (str out-time))
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

(defn delete-year [year]
  (when-let [year (first (get-years year))]
    (db/delete! year)))

(defn excuse-date [id date-string]
  (db/persist! {:type :excuses
                :student_id id
                :date date-string}))

(defn override-date [id date-string]
  (db/persist! {:type :overrides
                :student_id id
                :date date-string}))

;; (get-students )
(defn get-students
  ([] (db/get-* "students"))
  ([id] (db/get-* "students" id "_id")))

;; (get-years)    
(defn student-not-yet-created [name]
  (empty? (filter #(= name (:name %)) (get-students))))

(defn make-student [name]
  (when (student-not-yet-created name)
    (db/persist! {:type :students :name name :olderdate nil})))

(defn- toggle-older [older]
  (if older nil (str (t/now))))

(defn toggle-student [_id]
  (let [student (first (get-students _id))
        student (assoc student :olderdate (toggle-older (:olderdate student)))] 
    (db/update! :students _id {:olderdate (:olderdate student)})
    student))

(defn make-year [from to]
  (let [from (f/parse from)
        to (f/parse to)
        name (str (f/unparse date-format from) " "  (f/unparse date-format to))]
    (->> {:type :years :from_date (str from) :to_date (str to) :name name}
         db/persist!)))

;; (sample-db true)   
(defn sample-db
  ([] (sample-db false))
  ([have-extra?]
     (db/reset-db)
     (make-year (str (t/date-time 2014 6)) (str (t/date-time 2015 6)))
     (make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
     (let [s (make-student "jim")]
       (when have-extra? (swipe-in (:_id s) (t/minus (t/now) (t/days 2)))))
     (let [s (make-student "steve")]
       (when have-extra? (swipe-in (:_id s) (t/minus (t/now) (t/days 1) (t/hours 5))))))
  )

;; (huge-sample-db)
(defn huge-sample-db []
  (db/reset-db)
  (make-year (str (t/date-time 2014 6)) (str (t/date-time 2015 6)))
  (make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
  (loop [x 0]
    (if (> x 80)
      :done
      (do (let [s (make-student (str "zax" x))]
            (swipe-in (:_id s) (t/minus (t/now) (t/days 2))))
          (recur (inc x)))))
  (let [s (make-student "jim")]
    (swipe-in (:_id s) (t/minus (t/now) (t/days 2))))
  (let [s (make-student "steve")]
    (swipe-in (:_id s) (t/minus (t/now) (t/days 1) (t/hours 5))))
  )
