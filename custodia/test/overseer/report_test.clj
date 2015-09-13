(ns overseer.report-test
  (:require [clojure.test :refer :all]
            [overseer.helpers-test :refer :all]
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

;; TODO
;; D make report only select students from class
;; D migrate old reports to all have class
;; D migrate db to have a class
;; remove "absent" stats from student page, show total for all time
;; get student list from active class only
;; remove "archive" feature
;; D make reports have a class db
;; make reports have a class UI

(deftest swipe-attendence-with-class-test
  (do (data/sample-db)
      (let [
            class-id (get-class-id-by-name "2014-2015")
            today-str (dates/get-current-year-string (data/get-years))
            {sid :_id} (data/make-student "test")
            {sid2 :_id} (data/make-student "test2")
           ]
        (data/add-student-to-class sid class-id)
        (add-3good-2short-swipes sid)
        (data/swipe-in sid2 _10-19)
        (data/swipe-in sid2 _10-20)

        (let [att (db/get-report today-str)
              student1 (first (filter #(= sid (:_id %)) att))
              student2 (first (filter #(= sid2 (:_id %)) att))]
          (testing "Student 1 Counts"
            (is (= 3 (:good student1)))
            (is (= 25 (int (:total_hours student1))))
            (is (= 2 (:short student1))))
          (testing "Not absent on a day a different classed student attended"
            (is (= (:unexcused student1)
                   0)))
          (testing "Student 2 doesn't even show up"
            (is (= student2 nil)))
          )
        (testing "an older date string shows no attendance in that time"
          (is (= '() (db/get-report "06-01-2013 05-01-2014")))))) 
  )


(deftest swipe-attendence-test
  (do (data/sample-db)  
      (let [today-str (dates/get-current-year-string (data/get-years))
            class-id (get-class-id-by-name "2014-2015")
            {sid :_id} (data/make-student "test")
            {sid2 :_id} (data/make-student "test2")]
        ;; good today
        (add-3good-2short-swipes sid)
        (data/override-date sid "2014-10-18")
        (data/excuse-date sid "2014-10-20")

        (data/swipe-in sid2 _10-19)
        (data/swipe-in sid2 _10-20)

        (data/add-student-to-class sid class-id)
        (data/add-student-to-class sid2 class-id)

        (let [att (db/get-report today-str)
              student1 (first (filter #(= sid (:_id %)) att))
              student2 (first (filter #(= sid2 (:_id %)) att))]
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


