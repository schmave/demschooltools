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
  (case (:type swipe)
    "swipes" (make-date-string (or (:in_time swipe) (:out_time swipe)))
    (make-date-string-without-timezone (:date swipe))))

(defn get-min-minutes [student day]
  (let [older-date (-> student :olderdate c/from-sql-date)
        current-date  (f/parse day)]
    (if (and older-date
             current-date
             (t/before? older-date current-date))
      330 300)))

(defn filter-type [key col]
  (filter #(= (:type %) key) col))

(defn append-validity [student [day swipes]]
  (let [has-override? (boolean (seq (filter-type "overrides" swipes)))
        has-excuse? (boolean (seq (filter-type "excuses" swipes)))
        int-mins (->> swipes
                      (map :interval)
                      (filter number?)
                      (reduce +))
        min-minutes (get-min-minutes student day)]
    {:valid (and (not has-excuse?)
                 (or has-override?
                     (> int-mins min-minutes)))
     :short (or (and (not has-override?)
                     (not has-excuse?)
                     (seq swipes))
                (> int-mins 0)) 
     :override has-override?
     :excused has-excuse?
     :day day
     :total_mins (if has-override? min-minutes int-mins)
     :swipes swipes}))

(defn get-year-from-to [year-string]
  (let [year (first (get-years year-string))
        from (f/parse (:from_date year))
        to (f/parse (:to_date year))]
    [from to]))

(defn get-student-list []
  (let [today-string (make-date-string (t/now))
        inout (db/get-student-list-in-out)
        inout (map #(assoc % :in_today (= today-string (make-date-string (:last_swipe_date %)))) inout)]
    inout))  

(defn get-last-swipe-type [summed-days]
  (let [summed-days (filter #(not (nil? (:swipes %))) summed-days)]
    ;; (when (-> summed-days first :valid not))
    (let [day (-> summed-days first :day)
          swipes (-> summed-days first :swipes)
          swipes (filter #(= "swipes" (:type %)) swipes)
          last-swipe (last swipes)]
      (cond
       (:out_time last-swipe) ["out" day]
       (:in_time last-swipe) ["in" day]
       :else nil))))

(defn only-swipes-in-range [list from to]
  (let [interval (t/interval from to)]
    (filter #(or (t/within? interval
                            (f/parse (:in_time %)))
                 (t/within? interval
                            (f/parse (:out_time %))))
            list)))

(defn get-attendance [school-days year id student]
  (let [school-days (zipmap (reverse school-days) (repeat nil))
        [from to] (get-year-from-to year)
        swipes (db/get-swipes-in-year year id)
        swipes (concat swipes (db/get-overrides-in-year year id))
        swipes (concat swipes (db/get-excuses-in-year year id))
        grouped-swipes (group-by swipe-day swipes)
        ;; adding last swipe before absences
        grouped-swipes (merge school-days grouped-swipes)
        grouped-swipes (into (sorted-map) grouped-swipes)
        summed-days (map #(append-validity student %) grouped-swipes)
        summed-days (reverse summed-days)
        today-string (today-string)
        only-swipes (partial filter-type "swipes")
        in_today (and (= today-string
                         (-> summed-days first :day))
                      (-> summed-days first :swipes only-swipes count (> 0)))
        [last-swipe-type last-swipe-date] (get-last-swipe-type summed-days)]
    (merge student {:total_days (count (filter :valid summed-days))
                    :total_hours (/ (reduce + (map :total_mins summed-days))
                                    60)
                    :total_abs (count (filter #(and (-> % :valid not)
                                                    (-> % :excused not)
                                                    (-> % :short not))
                                              summed-days))
                    :total_short (count (filter #(and (-> % :valid not)
                                                      (:short %))
                                                summed-days))
                    :total_overrides (count (filter :override summed-days))
                    :total_excused (count (filter :excused summed-days))
                    :today today-string
                    :in_today in_today 
                    :last_swipe_type last-swipe-type
                    :last_swipe_date last-swipe-date
                    :days summed-days})))

(defn get-school-days [year]
  (map :days (filter :days (db/get-school-days year))))
;; (get-school-days "2014-06-01 2015-06-01")

(trace/deftrace get-students-with-att
  ([] (get-students-with-att (get-current-year-string (get-years)) nil))
  ([year] (get-students-with-att year nil))
  ([year id]
     (let [school-days (get-school-days year)]
       (sort-by :name
                (map #(get-attendance school-days year (:_id %) %)
                     (get-students id))))))

;; (get-students-with-att)

