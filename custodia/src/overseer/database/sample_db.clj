(ns overseer.database.sample-db
  (:require [clj-time.local :as l]
            [clj-time.core :as t]
            [clojure.test :refer :all]
            [overseer.db :as db]
            [clojure.tools.trace :as trace]
            [overseer.commands :as data]
            [overseer.database.connection :as conn]
            [overseer.database.users :as users]
            [overseer.attendance :as att]
            [clj-time.coerce :as c]))

(defn sample-db
  ([] (sample-db false))
  ([have-extra?]
   (conn/init-pg)
   (users/reset-db)
   (let [{class-id :_id} (data/get-class-by-name "2014-2015")]
     (db/activate-class class-id)
     (data/make-year (str (t/date-time 2014 6)) (str (t/plus (t/now) (t/days 9))))
     (data/make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
     (let [s (data/make-student "jim")
           {sid :_id} s]
       (data/add-student-to-class sid class-id)
       (when have-extra? (data/swipe-in sid (t/minus (t/now) (t/days 2)))))
     (let [s (data/make-student "steve")
           {sid :_id} s]
       (data/add-student-to-class sid class-id)
       (when have-extra? (data/swipe-in sid (t/minus (t/now) (t/days 1) (t/hours 5)))))))
  )
