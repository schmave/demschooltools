(ns overseer.attendance
  (:require [overseer.db :as db]
            [overseer.helpers :refer :all]
            [overseer.queries :as queries]
            [overseer.dates :refer :all]
            [overseer.commands :as cmd]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [schema.core :as s]
            [clj-time.coerce :as c]

            [clojure.tools.logging :as log]))
(def DayInformation
  {:student_id s/Num
   :day s/Str
   :requiredmin s/Num
   :has_excuse s/Bool
   :has_override s/Bool})

(def OverrideOrExcuse
  (merge DayInformation
         {:type (s/eq "")
          :nice_out_time (s/eq nil)
          :nice_in_time (s/eq nil)
          :intervalmin (s/eq nil)
          :olderdate (s/eq nil)
          :out_time (s/eq nil)
          :in_time (s/eq nil)
          :_id (s/eq nil)}))

(def Swipe
  (merge DayInformation
         {:_id s/Num
          :type (s/eq "swipes")
          :nice_out_time (s/maybe s/Str)
          :nice_in_time (s/maybe s/Str)
          :intervalmin (s/maybe s/Num)
          :olderdate (s/maybe java.sql.Date)
          :out_time (s/maybe java.sql.Timestamp)
          :in_time (s/maybe java.sql.Timestamp)}))

(def DayRecord (s/either OverrideOrExcuse Swipe))

(def StudentDay
  {:valid s/Bool
   :short s/Bool
   :absent s/Bool
   :override s/Bool
   :excused s/Bool
   :day s/Str
   :total_mins s/Num
   :swipes [DayRecord]}
  )

(def StudentPage
  "The student page types"
  {:total_short s/Num
   :_id s/Num
   :total_excused s/Num
   :total_hours s/Num
   :name s/Str
   :in_today s/Bool
   :today s/Str
   :days [StudentDay]
   :total_abs s/Num
   :type s/Keyword
   :total_overrides s/Num
   :total_days s/Num
   :absent_today s/Bool
   :inserted_date java.sql.Timestamp
   :show_as_absent (s/maybe s/Bool)
   :olderdate (s/maybe java.sql.Date)
   :last_swipe_type (s/maybe s/Str)
   :last_swipe_date (s/maybe s/Str)
   })

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
                     (>= int-mins min-minutes)))
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
  (let [year (first (queries/get-years year-string))
        from (f/parse (:from_date year))
        to (f/parse (:to_date year))]
    [from to]))

(defn get-student-list
  ([] (get-student-list false))
  ([show-archived]
   (let [today-string (make-date-string (t/now))
         inout (queries/get-student-list-in-out show-archived)
         _ (log/debug "student-list-in-out" inout)
         inout (map #(assoc %
                            :in_today (= today-string
                                         (make-date-string (:last_swipe_date %)))
                            :absent_today (= today-string
                                             (make-date-string-without-timezone (:show_as_absent %)))) inout)
         ]
     inout)))

(defn days-with-swipes [days]
  (filter (fn [d] (not (empty? (filter #(= "swipes" (:type %))
                                       (:swipes d)))))
          days))

(defn get-last-swipe-type [summed-days]
  (let [summed-days (days-with-swipes summed-days)]
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

(defn get-attendance
  [year id student]
  (let [swipes (queries/get-student-page id year)
        swipes (map #(assoc % :day (-> % :day make-date-string-without-timezone)
                            :has_override (boolean (:has_override %))
                            :has_excuse (boolean (:has_excuse %)))
                    swipes)
        grouped-swipes (group-by :day swipes)
        grouped-swipes (into (sorted-map) grouped-swipes)
        summed-days (map #(append-validity student %) grouped-swipes)
        summed-days (reverse summed-days)
        today-string  (today-string)
        absent_today (= today-string  (make-date-string-without-timezone
                                       (:show_as_absent student)))
        in_today (and (= today-string
                         (-> summed-days first :day))
                      (-> summed-days first :swipes only-swipes count (> 0)))
        [last-swipe-type last-swipe-date] (get-last-swipe-type summed-days)
        total_minutes (->> summed-days (map :total_mins) (reduce +))]
    (merge student {:total_days (count (filter :valid summed-days))
                    :total_hours (with-precision 5 (/ total_minutes 60))
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

;; (queries/get-student-page 3 (get-current-year-string (queries/get-years)))

(defn get-student-with-att
  ([id] (get-student-with-att id (get-current-year-string (queries/get-years))))
  ([id year]
   (map #(get-attendance year (:_id %) %)
        (queries/get-students id))))

;;(get-student-with-att 8)
;; (get-current-year-string (queries/get-years))
