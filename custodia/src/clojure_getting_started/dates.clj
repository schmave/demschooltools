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

(def date-format (f/formatter "MM-dd-yyyy"))
(def time-format (f/formatter "hh:mm:ss"))
;; TODO make this configurable?
(def local-time-zone-id (t/time-zone-for-id "America/New_York"))

(defn format-to-local [f d]
  (f/unparse (f/with-zone f local-time-zone-id) d))

(defn parse-date-string [d]
  (f/parse date-format d))

(defn make-date-string [d]
  (when d (format-to-local date-format (f/parse d))))

(defn make-time-string [d]
  (when d (format-to-local time-format (f/parse d))))

(defn clean-dates [swipe]
  (?assoc swipe
          :nice_in_time (make-time-string (:in_time swipe))
          :nice_out_time (make-time-string (:out_time swipe))))

(defn only-dates-between [list f dfrom dto]
  (filter #(t/within? (t/interval dfrom dto)
                      (f/parse (f %)))
          list))

(defn append-interval [swipe]
  (if (:out_time swipe)
    (let [int (t/interval (f/parse (:in_time swipe))
                          (f/parse (:out_time swipe)))
          int-hours (t/in-minutes int)]
      (assoc swipe :interval int-hours))
    swipe))

(defn get-current-year-string [years]
  (->> years
       (filter #(t/within? (t/interval (f/parse (:from %))
                                       (f/parse (:to %)))
                           (t/now)))
       first
       :name))
