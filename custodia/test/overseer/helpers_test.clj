(ns overseer.helpers-test
  (:require [clj-time.local :as l]
            [clj-time.core :as t]
            [clojure.test :refer :all]
            [overseer.db :as db]
            [overseer.queries :as queries]
            [clojure.tools.logging :as log]
            [overseer.commands :as cmd]
            [overseer.database.users :as users]
            [overseer.database.connection :as conn]
            [overseer.attendance :as att]
            [clj-time.coerce :as c]))


(defn today-at-utc [h m]
  (t/plus (t/today-at h m) (t/hours 4)))

(defn get-att [id]
  (first (att/get-student-with-att id)))

(def _2014_10-14_9-14am (t/date-time 2014 10 14 14 9 27 246)) 
(def _2014_10_15 (t/plus _2014_10-14_9-14am (t/days 1)))
(def _2014_10_16 (t/plus _2014_10-14_9-14am (t/days 2)))
(def _2014_10_17 (t/plus _2014_10-14_9-14am (t/days 3)))
(def _2014_10_18 (t/plus _2014_10-14_9-14am (t/days 4)))
(def _2014_10_19 (t/plus _2014_10-14_9-14am (t/days 5)))
(def _2014_10_20 (t/plus _2014_10-14_9-14am (t/days 6)))


(defn add-3good-2short-swipes [sid]
  ;; good

  (cmd/swipe-in sid _2014_10-14_9-14am)
  (cmd/swipe-out sid (t/plus _2014_10-14_9-14am (t/hours 6)))

  ;; good

  (cmd/swipe-in sid _2014_10_15)
  (cmd/swipe-out sid (t/plus _2014_10_15 (t/hours 6)))

  ;; short

  (cmd/swipe-in sid _2014_10_16)
  (cmd/swipe-out sid (t/plus _2014_10_16 (t/hours 4)))

  ;; good = two short segments

  (cmd/swipe-in sid _2014_10_17)
  (cmd/swipe-out sid (t/plus _2014_10_17 (t/hours 4)))
  (cmd/swipe-in sid (t/plus _2014_10_17 (t/hours 4.1)))
  (cmd/swipe-out sid (t/plus _2014_10_17 (t/hours 6)))

  ;; short

  (cmd/swipe-in sid _2014_10_18)
  (cmd/swipe-out sid (t/plus _2014_10_18 (t/hours 4)))
  )

(defn get-class-id-by-name [name]
  (:_id (cmd/get-class-by-name name)))

(defn make-sample-two-students-in-class []
  (let [{class-id :_id} (cmd/get-class-by-name "2014-2015")]
    (cmd/activate-class class-id)
    (cmd/make-year (str (t/date-time 2014 6)) (str (t/plus (t/now) (t/days 2))))
    (cmd/make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
    (let [s (cmd/make-student "jim2")
          {sid :_id} s
          result {:class_id class-id :student_ids [sid]}]
      (cmd/add-student-to-class sid class-id)
      (let [s (cmd/make-student "steve2")
            {sid :_id} s
            result (update-in result [:student_ids] (fn [sids] (conj sids sid)))]
        (cmd/add-student-to-class sid class-id)
        result))))

(defn student-report-is [att good short excuses unexcused overrides hours]
  (testing "Student attendence"
    (is (= good (:good att)) "good")
    (is (= short (:short att)) "short")
    (is (= excuses (:excuses att)) "excuses")
    (is (= overrides (:overrides att)) "overrides")
    (is (= hours (:total_hours att)) "hours")
    (is (= unexcused (:unexcused att))) "unexcused"))

(defn student-att-is [att total abs overrides short]
  (testing "Student attendence"
    (is (= total (:total_days att)) "Total days")
    (is (= abs (:total_abs att)) "Total Abs")
    (is (= overrides (:total_overrides att)) "Total overrides")
    (is (= short (:total_short att))) "Total short"))

;; (sample-db true) 
;; (binding [db/*school-id* 1] (sample-db true))
(defn sample-db
  ([] (sample-db false))
  ([have-extra?]
   (conn/init-pg)
   (users/reset-db)
   (let [{class-id :_id} (cmd/get-class-by-name "2014-2015")]
     (cmd/activate-class class-id)
     (cmd/make-year (str (t/date-time 2014 6)) (str (t/plus (t/now) (t/days 9))))
     (cmd/make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
     (let [s (cmd/make-student "jim")
           {sid :_id} s]
       (cmd/add-student-to-class sid class-id)
       (when have-extra? (cmd/swipe-in sid (t/minus (t/now) (t/days 2)))))
     (let [s (cmd/make-student "steve")
           {sid :_id} s]
       (cmd/add-student-to-class sid class-id)
       (when have-extra? (cmd/swipe-in sid (t/minus (t/now) (t/days 1) (t/hours 5)))))))
  )



;; (huge-sample-db) 
(defn huge-sample-db []
  (conn/init-pg)
  (users/reset-db)
  (cmd/make-year (str (t/date-time 2014 6)) (str (t/plus (t/now) (t/days 5))))  
  (cmd/make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))

  (let [{class-id :_id} (cmd/get-class-by-name "2014-2015")]
    (cmd/activate-class class-id)
    (loop [x 1]
      (if (> x 80)
        :done
        (do (let [s (cmd/make-student (str "zax" x))
                  {sid :_id} s]
              (cmd/add-student-to-class sid class-id)
              (loop [y 2]
                (log/info (str "Id:" x " Num:" y " of:" (* 80 200)))
                (if (> y 200)
                  :done
                  (do
                    (cmd/swipe-in x (t/minus (today-at-utc 9 0) (t/days y)))
                    (cmd/swipe-out x (t/minus (t/plus (today-at-utc 9 0) (t/hours 6))
                                               (t/days y)))
                    (recur (inc y))))))
            (recur (inc x))))))
  )
