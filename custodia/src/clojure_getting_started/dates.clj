(ns clojure-getting-started.dates
  (:require [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clojure-getting-started.helpers :refer :all]
            [clojure.tools.trace :as trace]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            ))

(def date-format (f/formatter "yyyy-MM-dd"))
(def time-format (f/formatter "hh:mm:ss"))
;; TODO make this configurable?
(def local-time-zone-id (t/time-zone-for-id "America/New_York"))

(defn format-to-local [f d]
  (f/unparse (f/with-zone f local-time-zone-id) d))

(defn parse-date-string [d]
  (f/parse date-format d))

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

(defn make-time-string [d]
  (when d
    (cond (instance? org.joda.time.DateTime d) (format-to-local time-format d)
          (instance? java.lang.String d) (format-to-local time-format (f/parse d))
          :else (format-to-local time-format (c/from-sql-time d)))))

(defn today-string []
  (format-to-local date-format (t/now)))

(defn clean-dates [swipe]
  (?assoc swipe
          :nice_in_time (make-time-string (:in_time swipe))
          :nice_out_time (make-time-string (:out_time swipe))))

(defn only-dates-between [list f dfrom dto]
  (filter #(t/within? (t/interval dfrom dto)
                      (f/parse (f %)))
          list))

(defn append-interval [swipe]
  (if (and (:in_time swipe)
           (:out_time swipe))
    (let [int (t/interval (f/parse (:in_time swipe))
                          (f/parse (:out_time swipe)))
          int-hours (t/in-minutes int)]
      (assoc swipe :interval int-hours))
    swipe))

(defn get-current-year-string [years]
  (->> years
       (filter #(t/within? (t/interval (c/from-sql-time (:from_date %))
                                       (c/from-sql-time (:to_date %)))
                           (t/now)))
       first
       :name))

