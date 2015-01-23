(ns clojure-getting-started.attendance
  (:require [clojure-getting-started.db :as db]
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


(defn get-min-minutes [student day]
  (let [older-date (-> student :olderdate f/parse)
        current-date (f/parse day)]
    (if (and older-date
             current-date
             (t/before? older-date current-date))
      330 300)))

(defn append-validity [student [day swipes]]
  (let [has-override? (->> swipes
                           (filter #(= (:type %) "override"))
                           not-empty
                           boolean)
        int-mins (->> swipes
                      (map (comp :interval))
                      (filter (comp not nil?))
                      (reduce +))
        min-minutes (get-min-minutes student day)]
    {:valid (or has-override? (> int-mins min-minutes))
     :override has-override?
     :day day
     :total_mins int-mins
     :swipes swipes}))

(defn get-year-from-to [year-string]
  (let [year (first (get-years year-string))
        from (f/parse (:from year))
        to (f/parse (:to year))]
    [from to]))

(defn get-last-swipe-type [summed-days]
  (let [summed-days (filter #(not (nil? (:swipes %))) summed-days)]
    (when (-> summed-days first :valid not)
      (let [day (-> summed-days first :day)
            swipes (-> summed-days first :swipes)
            swipes (filter #(= "swipe" (:type %)) swipes)
            last-swipe (last swipes)]
        (cond
         (:out_time last-swipe) ["out" day]
         (:in_time last-swipe) ["in" day]
         :else nil)))))

(defn get-attendance [school-days year id student]
  (let [school-days (zipmap (reverse school-days) (repeat nil))
        [from to] (get-year-from-to year)
        swipes (get-swipes id)
        swipes (only-dates-between swipes :in_time from to)
        swipes (map append-interval swipes)
        swipes (map clean-dates swipes)
        swipes (concat swipes (only-dates-between (get-overrides id) :date from to))
        grouped-swipes (group-by swipe-day swipes)
        ;; adding last swipe before absences
        grouped-swipes (merge school-days grouped-swipes)
        grouped-swipes (into (sorted-map) grouped-swipes)
        summed-days (map #(append-validity student %) grouped-swipes)
        summed-days (reverse summed-days)
        today-string (format-to-local date-format (t/now))
        in_today (and (= today-string
                         (-> summed-days first :day))
                      (-> summed-days first :swipes count (> 0)))
        [last-swipe-type last-swipe-date] (get-last-swipe-type summed-days)]
    (merge student {:total_days (count (filter :valid summed-days))
                    :total_abs (count (filter (comp not :valid) summed-days))
                    :total_overrides (count (filter :override summed-days))
                    :today today-string
                    :in_today in_today 
                    :last_swipe_type last-swipe-type
                    :last_swipe_date last-swipe-date
                    :days summed-days})))

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
       (sort-by :name
                (map #(get-attendance school-days year (:_id %) %)
                     (get-students ids))))))
