(ns overseer.attendance
  (:require [overseer.db :as db]
            [overseer.helpers :refer :all]
            [overseer.dates :refer :all]
            [overseer.database :refer :all]
            [clojure.tools.trace :as trace]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            ))

(defn only-swipes [s]
  (filter #(or (:out_time %)
               (:in_time %)) s))

(defn swipe-day [swipe]
  (case (:type swipe)
    "swipes" (make-date-string (or (:in_time swipe) (:out_time swipe)))
    (make-date-string-without-timezone (:date swipe))))

(defn filter-type [key col]
  (filter #(= (:type %) key) col))

(defn append-validity [student [day records]]
  (let [has-override? (-> records first :has_override boolean)
        has-excuse? (-> records first :has_excuse boolean)
        int-mins (reduce #(+ %1 (or (:intervalmin %2) 0))
                         0 records)
        min-minutes (-> records first :requiredmin)]
    {:valid (and (not has-excuse?)
                 (or has-override?
                     (> int-mins min-minutes)))
     :short (boolean (or (and (not has-override?)
                              (not has-excuse?)
                              (seq (only-swipes records)))
                         ;; what is this here for?
                         (> int-mins 0))) 
     :absent (boolean (and (not has-override?)
                           (not has-excuse?)
                           (not (seq (only-swipes records))))) 
     :override has-override?
     :excused has-excuse?
     :day day
     :total_mins (if has-override? min-minutes int-mins)
     :swipes records}))

(defn get-year-from-to [year-string]
  (let [year (first (get-years year-string))
        from (f/parse (:from_date year))
        to (f/parse (:to_date year))]
    [from to]))

(defn get-student-list []
  (let [today-string (make-date-string (t/now))
        inout (db/get-student-list-in-out)
        inout (map #(assoc %
                      :in_today (= today-string
                                   (make-date-string (:last_swipe_date %)))
                      :absent_today (= today-string
                                       (make-date-string-without-timezone (:show_as_absent %)))) inout)
        ]
    inout))  

(defn get-last-swipe-type [summed-days]
  (let [summed-days (filter #(not (nil? (:swipes %))) summed-days)]
    (let [day (-> summed-days first :day)
          records (-> summed-days first :swipes)
          swipes (only-swipes records)
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

(defn get-attendance [year id student]
  (let [swipes (db/get-student-page id year)
        swipes (map #(assoc % :day (-> % :day make-date-string-without-timezone)) swipes)
        grouped-swipes (group-by :day swipes)
        summed-days (map #(append-validity student %) grouped-swipes)
        summed-days (reverse summed-days)
        today-string (today-string)
        absent_today (= today-string (make-date-string-without-timezone
                                      (:show_as_absent student)))
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
                    :absent_today (boolean absent_today)
                    :in_today in_today 
                    :last_swipe_type last-swipe-type
                    :last_swipe_date last-swipe-date
                    :days summed-days})))

(defn get-school-days [year]
  (map :days (filter :days (db/get-school-days year))))
;; (get-school-days "2014-06-01 2015-06-01")

(defn get-student-with-att
  ([id] (get-student-with-att id (get-current-year-string (get-years))))
  ([id year] 
     (sort-by :name
              (map #(get-attendance year (:_id %) %)
                    (get-students id)))))

;; (get-students-with-att)

