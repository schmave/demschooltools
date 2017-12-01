(ns overseer.dates
  (:require [com.ashafa.clutch :as couch]
            [overseer.db :as db]
            [overseer.helpers :refer :all]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [schema.core :as s]))

(def DateTime org.joda.time.DateTime)

(def date-format (f/formatter "yyyy-MM-dd"))
(def time-format (f/formatter "hh:mm:ss"))

(def local-time-zone-id (t/time-zone-for-id db/*school-timezone*))

(defn format-to-local [f d]
  (f/unparse (f/with-zone f local-time-zone-id) d))

(defn parse-date-string [d]
  (f/parse date-format d))

(defn cond-parse-date-string [d]
  (if (instance? java.lang.String d)
    (f/parse (f/formatters :date-time) d)
    d))

(defn calculate-interval [swipe]
  (let [out-time (-> swipe :rounded_out_time c/from-sql-time)
        in-time (-> swipe :rounded_in_time c/from-sql-time)]
    (if (and out-time in-time)
      (t/in-minutes (t/interval in-time out-time))
      0M)))

(defn from-sql-time [inst]
  (f/unparse (f/with-zone time-format local-time-zone-id) (c/from-sql-date inst)))

(defn make-date-string-without-timezone [d]
  (when d
    (if (instance? org.joda.time.DateTime d)
      (f/unparse date-format d)
      (f/unparse date-format (c/from-sql-date d)))))

(defn make-date-string [d]
  (when d
    (cond
     (instance? org.joda.time.DateTime d) (format-to-local date-format d)
     (instance? java.lang.String d) (format-to-local date-format (f/parse d))
     :else (format-to-local date-format (c/from-sql-time d)))))

(defn today-string []
  (format-to-local date-format (t/now)))

(defn only-dates-between [list f dfrom dto]
  (filter #(t/within? (t/interval dfrom dto)
                      (f/parse (f %)))
          list))

(defn in-local-time-at-hour [date hour]
  (let [local-date (f/parse (make-date-string date))]
    (-> (t/date-time (t/year local-date) (t/month local-date) (t/day local-date) hour 0)
        (t/from-time-zone local-time-zone-id))))

(defn earliest-time [date] (in-local-time-at-hour date 9))

(defn latest-time [date] (in-local-time-at-hour date 16))

(s/defn round-swipe-time :- DateTime [time]
  (let [time (cond-parse-date-string time)
        early (earliest-time time)
        late (latest-time time)
        time (if (t/after? time early) time early)
        time (if (t/before? time late) time late)]
    time))

(defn append-interval [swipe]
  (if (and (:in_time swipe)
           (:out_time swipe))
    (let [int (t/interval (f/parse (:in_time swipe))
                          (f/parse (:out_time swipe)))
          int-hours (t/in-minutes int)]
      (assoc swipe :interval int-hours))
    swipe))


(defn get-current-year-string [years]
  (let [current_year (->> years
                          (filter #(t/within? (t/interval (c/from-sql-time (:from_date %))
                                                          (c/from-sql-time (:to_date %)))
                                              (t/now)))
                          first
                          :name)]
    (if current_year
      current_year
      (-> years last :name))))
