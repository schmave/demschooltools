(ns overseer.helpers-test
  (:require [clj-time.local :as l]
            [clj-time.core :as t]
            [overseer.database :as data]
            [overseer.attendance :as att]
            [clj-time.coerce :as c]))

(defn today-at-utc [h m]
  (t/plus (t/today-at h m) (t/hours 4)))

(defn get-att [id]
  (first (att/get-student-with-att id)))

(def _10-14_9-14am (t/date-time 2014 10 14 14 9 27 246)) 
(def _10-15 (t/plus _10-14_9-14am (t/days 1)))
(def _10-16 (t/plus _10-14_9-14am (t/days 2)))
(def _10-17 (t/plus _10-14_9-14am (t/days 3)))
(def _10-18 (t/plus _10-14_9-14am (t/days 4)))
(def _10-19 (t/plus _10-14_9-14am (t/days 5)))
(def _10-20 (t/plus _10-14_9-14am (t/days 6)))

(defn add-3good-2short-swipes [sid]
  ;; good

  (data/swipe-in sid _10-14_9-14am)
  (data/swipe-out sid (t/plus _10-14_9-14am (t/hours 6)))

  ;; good

  (data/swipe-in sid _10-15)
  (data/swipe-out sid (t/plus _10-15 (t/hours 6)))

  ;; short

  (data/swipe-in sid _10-16)
  (data/swipe-out sid (t/plus _10-16 (t/hours 4)))

  ;; good = two short segments

  (data/swipe-in sid _10-17)
  (data/swipe-out sid (t/plus _10-17 (t/hours 4)))
  (data/swipe-in sid (t/plus _10-17 (t/hours 4.1)))
  (data/swipe-out sid (t/plus _10-17 (t/hours 6)))

  ;; short

  (data/swipe-in sid _10-18)
  (data/swipe-out sid (t/plus _10-18 (t/hours 4)))
  )

(defn get-class-id-by-name [name]
  (:_id (data/get-class-by-name name)))
