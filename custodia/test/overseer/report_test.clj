(ns overseer.report-test
  (:require [clojure.test :refer :all]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.trace :as trace]
            [overseer.db :as db]
            [overseer.database :as data]
            [overseer.attendance :as att]
            [overseer.helpers :as h]
            [overseer.dates :as dates]
            ))

(def basetime (t/date-time 2014 10 14 14 9 27 246)) 
(defn add-swipes [sid]
  ;; 14 hours in UTC is 9 Am here
  (data/swipe-in sid basetime)
  (data/swipe-out sid (t/plus basetime (t/hours 6)))

  ;; good tomorrow
  
  (data/swipe-in sid (t/plus basetime (t/days 1)))
  (data/swipe-out sid (t/plus basetime (t/days 1) (t/hours 6)))

  ;; short the next
  
  (data/swipe-in sid (t/plus basetime (t/days 2)))
  (data/swipe-out sid (t/plus basetime (t/days 2) (t/hours 4)))


  ;; two short the next but long enough
  
  (data/swipe-in sid (t/plus basetime (t/days 3)))
  (data/swipe-out sid (t/plus basetime (t/days 3) (t/hours 4)))
  (data/swipe-in sid (t/plus basetime (t/days 3) (t/hours 4.1)))
  (data/swipe-out sid (t/plus basetime (t/days 3) (t/hours 6)))

  ;; short the next - 10-18-2014 
  
  (data/swipe-in sid (t/plus basetime (t/days 4)))
  (data/swipe-out sid (t/plus basetime (t/days 4) (t/hours 4)))
  )

(defn get-class-id-by-name [name]
  (:_id (data/get-class-by-name name)))

;; TODO
;; make report only select students from class
;; migrate old reports to all have class
;; migrate db to have a class
;; remove "absent" stats from student page, show total for all time
;; get student list from active class only
;; remove "archive" feature
;; make reports have a class
(deftest swipe-attendence-with-class-test
  (do (data/sample-db)
      (let [class-id (get-class-id-by-name "2014-2015")
            today-str (dates/get-current-year-string (data/get-years))
            s (data/make-student "test")
            sid (:_id s)
            s2 (data/make-student "test2")
            sid2 (:_id s2)]
        (data/add-student-to-class sid class-id)
        (add-swipes sid)
        (data/override-date sid "2014-10-18")
        (data/excuse-date sid "2014-10-20")
        (data/swipe-in sid2 (t/plus basetime (t/days 5)))
        (data/swipe-in sid2 (t/plus basetime (t/days 6)))

        (let [att (db/get-report today-str)
              student1 (first (filter #(= (:_id s) (:_id %)) att))
              student2 (first (filter #(= (:_id s2) (:_id %)) att))]
          (testing "Total Valid Day Count"
            (is (= (:good student1)
                   4)))
          (testing "Total Short Day Count"
            (is (= (:short student1)
                   1)))
          (testing "Total Excused Count"
            (is (= (:excuses student1)
                   1)))
          (testing "Total Abs Count"
            (is (= (:unexcused student1)
                   1)))
          (testing "Total Overrides"
            (is (= (:overrides student1)
                   1)))
          (testing "Total Hours"
            (is (= (int (:total_hours student1))
                   26)))
          (testing "Student 2 doesn't even show up"
            (is (= student2 nil)))
          )
        (testing "an older date string shows no attendance in that time"
          (is (= '() (db/get-report "06-01-2013 05-01-2014")))))) 
  )


(deftest swipe-attendence-test
  (do (data/sample-db)  
      (let [today-str (dates/get-current-year-string (data/get-years))
            s (data/make-student "test")
            sid (:_id s) 
            s2 (data/make-student "test2")
            sid2 (:_id s2)]
        ;; good today
        (add-swipes sid)
        (data/override-date sid "2014-10-18")
        (data/excuse-date sid "2014-10-20")
        (data/swipe-in sid2 (t/plus basetime (t/days 5)))
        (data/swipe-in sid2 (t/plus basetime (t/days 6)))

        (let [att (db/get-report today-str)
              student1 (first (filter #(= (:_id s) (:_id %)) att))
              student2 (first (filter #(= (:_id s2) (:_id %)) att))]
          (testing "Total Valid Day Count"
            (is (= (:good student1)
                   4)))
          (testing "Total Short Day Count"
            (is (= (:short student1)
                   1)))
          (testing "Total Excused Count"
            (is (= (:excuses student1)
                   1)))
          (testing "Total Abs Count"
            (is (= (:unexcused student1)
                   1)))
          (testing "Total Overrides"
            (is (= (:overrides student1)
                   1)))
          (testing "Total Hours"
            (is (= (int (:total_hours student1))
                   26)))
          (testing "Total short count student 2"
            ;; TODO - determine correct short count (otherwise 2)
            (is (= (:short student2)
                   0)))
          (testing "Total Abs Count For Student 2 Should equal number of total days for student 1 and 2"
            (is (= (:unexcused student2)
                   5)))
          )
        (testing "an older date string shows no attendance in that time"
          (is (= '() (db/get-report "06-01-2013 05-01-2014")))))) 
  )


