 (ns overseer.helpers-test
  (:require [clj-time.local :as l]
            [clj-time.core :as t]
            [clojure.test :refer :all]
            [overseer.db :as db]
            [clojure.tools.logging :as log]
            [overseer.database :as data]
            [overseer.database.connection :as conn]
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

(defn make-sample-two-students-in-class []
  (let [{class-id :_id} (data/get-class-by-name "2014-2015")]
    (db/activate-class class-id)
    (data/make-year (str (t/date-time 2014 6)) (str (t/plus (t/now) (t/days 2))))
    (data/make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
    (let [s (data/make-student "jim2")
          {sid :_id} s
          result {:class_id class-id :student_ids [sid]}]
      (data/add-student-to-class sid class-id)
      (let [s (data/make-student "steve2")
            {sid :_id} s
            result (update-in result [:student_ids] (fn [sids] (conj sids sid)))]
        (data/add-student-to-class sid class-id)
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
;; (binding [db/*school-schema* "demo"] (sample-db true))
(defn sample-db
  ([] (sample-db false))
  ([have-extra?]
   (conn/init-pg)
   (db/reset-db)
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

;; (huge-sample-db) 
(defn huge-sample-db []
  (conn/init-pg)
  (db/reset-db)
  (data/make-year (str (t/date-time 2014 6)) (str (t/plus (t/now) (t/days 5))))  
  (data/make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
  (loop [x 1]
    (if (> x 80)
      :done
      (do (let [s (data/make-student (str "zax" x))]
            (loop [y 2]
              (log/info (str "Id:" x " Num:" y " of:" (* 80 200)))
              (if (> y 200)
                :done
                (do
                  (data/swipe-in x (t/minus (t/now) (t/days y)))
                  (data/swipe-out x (t/minus (t/plus (t/now) (t/minutes 5))
                                        (t/days y)))
                  (recur (inc y))))))

          (recur (inc x)))))
  )
