(ns clojure-getting-started.attendance
  (:require [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clojure-getting-started.helpers :refer :all]
            [clojure-getting-started.dates :refer :all]
            [clojure-getting-started.database :refer :all]
            [clojure.tools.trace :as trace]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            ))

(defn swipe-day [swipe]
  (if (:in_time swipe)
    (make-date-string (:in_time swipe))
    (:date swipe)))

(defn get-hours-needed [id]
  ;; TODO implement this
  5)

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

(defn get-year-from-to [year-string]
  (let [year (first (get-years year-string))
        from (f/parse (:from year))
        to (f/parse (:to year))]
    [from to]))

(defn get-attendance [school-days year id]
  (let [school-days (zipmap (reverse school-days) (repeat nil))
        [from to] (get-year-from-to year)
        min-hours (get-hours-needed id)
        swipes (get-swipes id)
        swipes (only-dates-between swipes :in_time from to)
        swipes (map append-interval swipes)
        swipes (map clean-dates swipes)
        swipes (concat swipes (only-dates-between (get-overrides id) :date from to))
        grouped-swipes (group-by swipe-day swipes)
        grouped-swipes (merge school-days grouped-swipes)
        grouped-swipes (into (sorted-map) grouped-swipes)
        summed-days (map #(append-validity min-hours %) grouped-swipes)
        summed-days (reverse summed-days)
        today-string (format-to-local date-format (t/now))
        swiped-in-today? (-> summed-days first :day (= today-string))]
    {:total_days (count (filter :valid summed-days))
     :total_abs (count (filter (comp not :valid) summed-days))
     :total_overrides (count (filter :override summed-days))
     :today swiped-in-today?
     :days summed-days}))

(defn get-school-days [year]
  (let [[from to] (get-year-from-to year)
        swipes (get-swipes)
        swipes (only-dates-between swipes :in_time from to)]
    (keys (group-by swipe-day swipes))))

(defn get-students-with-att
  ([] (get-students-with-att (get-current-year-string (get-years)) nil))
  ([year] (get-students-with-att year nil))
  ([year ids]
     (let [school-days (get-school-days year)]
       (->> (get-students ids)
            (map #(merge (get-attendance school-days year (:_id %)) %))))))
