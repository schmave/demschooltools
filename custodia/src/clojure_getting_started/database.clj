(ns clojure-getting-started.database
  (:require [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clojure-getting-started.helpers :refer :all]
            [clojure.tools.trace :as trace]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            ))

(defn get-* [type ids]
  (map :value
       (if ids
         (couch/get-view db/db "view" type {:keys (if (coll? ids) ids [ids])})
         (couch/get-view db/db "view" type))))

(defn get-swipes [ids]
  (get-* "swipes" ids))

(defn get-overrides [ids]
  (get-* "overrides" ids))

(defn- lookup-last-swipe [id]
  (-> (get-swipes id)
      last))

(defn only-swiped-in? [in-swipe] (and in-swipe (not (:out_time in-swipe))))

(defn swipe-in
  ([id] (swipe-in id (t/now)))
  ([id time] 
     (let [last-swipe (lookup-last-swipe id)]
       (if (only-swiped-in? last-swipe)
         (+ 1 1);; (ask-for-out-swipe)
         (couch/put-document db/db
                             {:type :swipe :student_id id :in_time (str time)})))))

(defn- ask-for-in-swipe [id] (swipe-in id))

(defn swipe-out
  ([id] (swipe-out id (t/now)))
  ([id time] (let [last-swipe (lookup-last-swipe id)]
               (if (only-swiped-in? last-swipe)
                 (couch/put-document db/db (assoc last-swipe :out_time (str time)))
                 #_(let [in-swipe (ask-for-in-swipe id)]
                     (couch/put-document db/db (assoc in-swipe :out_time (str time))))))))

(defn get-hours-needed [id]
  ;; TODO implement this
  5)

(def date-format (f/formatter "MM-dd-yyyy"))
(def time-format (f/formatter "hh:mm:ss"))
;; TODO make this configurable?
(def local-time-zone-id (t/time-zone-for-id "America/New_York"))

(defn format-to-local [d f]
  (f/unparse (f/with-zone f local-time-zone-id)
             (f/parse d)))

(defn parse-date-string [d]
  (f/parse date-format d))

(defn make-date-string [d]
  (when d (format-to-local d date-format)))

(defn make-time-string [d]
  (when d (format-to-local d time-format)))

(defn clean-dates [swipe]
  (?assoc swipe
          :nice_in_time (make-time-string (:in_time swipe))
          :nice_out_time (make-time-string (:out_time swipe))))

(defn append-validity [min-hours swipes]
  (let [has-override? (->> swipes
                           second
                           (filter #(= (:type %) "override"))
                           not-empty
                           boolean)
        int-mins (->> swipes
                      second
                      (map (comp :interval))
                      (filter (comp not nil?))
                      (reduce +))
        int-hours (/ int-mins 60)]
    {:valid (or has-override? (> int-hours min-hours))
     :override has-override?
     :day (first swipes)
     :total_mins int-mins
     :swipes (second swipes)}))

;; TODO - make multimethod on type
(defn swipe-day [swipe]
  (if (:in_time swipe)
    (make-date-string (:in_time swipe))
    (:date swipe)))

(defn append-interval [swipe]
  (if (:out_time swipe)
    (let [int (t/interval (f/parse (:in_time swipe))
                          (f/parse (:out_time swipe)))
          int-hours (t/in-minutes int)]
      (assoc swipe :interval int-hours))
    swipe))

(defn get-years
  ([] (get-years nil))
  ([names]
     (get-* "years" names)))

(defn only-dates-between [list f dfrom dto]
  (filter #(t/within? (t/interval dfrom dto)
                      (f/parse (f %)))
          list))

(defn get-attendance [year id]
  (let [year (first (get-years year))
        from (f/parse (:from year))
        to (f/parse (:to year))
        min-hours (get-hours-needed id)
        swipes (get-swipes id)
        swipes (only-dates-between swipes :in_time from to)
        swipes (map append-interval swipes)
        swipes (map clean-dates swipes)
        swipes (concat swipes (only-dates-between (get-overrides id) :date from to))
        grouped-swipes (group-by swipe-day swipes)
        summed-days (map #(append-validity min-hours %) grouped-swipes)]
    {:total_days (count (filter :valid summed-days))
     :total_abs (count (filter (comp not :valid) summed-days))
     :total_overrides (count (filter :override summed-days))
     :days (reverse summed-days)}))

(defn override-date [id date-string]
  (->> {:type :override
        :student_id id
        :date date-string}
       (couch/put-document db/db)))

(defn get-students
  ([] (get-students nil))
  ([ids] (get-* "students" ids)))

(defn get-students-with-att
  ([year] (get-students-with-att year nil))
  ([year ids]
     (map #(merge (get-attendance year (:_id %)) %)
          (get-students ids))))

;; (get-years)    
(defn student-not-yet-created [name]
  (empty? (filter #(= name (:name %)) (get-students))))

(defn make-student [name]
  (when (student-not-yet-created name)
    (couch/put-document db/db {:type :student :name name})))

(defn make-year [from to]
  (let [from (f/parse from)
        to (f/parse to)
        name (str (t/year from) "-" (t/year to))]
    (->> {:type :year :from (str from) :to (str to) :name name}
         (couch/put-document db/db))))

;; (sample-db)   
(defn sample-db []
  (couch/delete-database db/db)
  (couch/create-database db/db)
  (db/make-db)
  (make-year (str (t/date-time 2014 6)) (str (t/date-time 2015 6)))
  (make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
  (make-student "steve")
  (make-student "jim")
  ;; (swipe-out 1)
  ;; (get-students)
  ;; (get-students)
  ;; (get-students "steve")
  )
