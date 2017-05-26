(ns overseer.report-test
  (:require [clojure.test :refer :all]
            [overseer.helpers-test :refer :all]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.trace :as trace]
            [overseer.db :as db]
            [overseer.commands :as cmd]
            [overseer.helpers-test :refer :all]
            [overseer.attendance :as att]
            [overseer.helpers :as h]
            [overseer.dates :as dates]
            ))

;; TODO
;; D CRUD classes and students to them
;; D migrate old reports to all have class
;; D migrate db to have a class
;; remove "absent" stats from student page, show total for all time
;; D get student list from active class only
;; remove "archive" feature
;; make reports have a class db
;; make report only select students from class
;; make reports have a class UI

(deftest swipe-attendence-with-class-test
  "Report is defined without a class but will use the default one"
  (do (sample-db)
      (let [class-id (get-class-id-by-name "2014-2015")
            other-class (cmd/make-class "test")
            activated (db/activate-class (:_id other-class))
            today-str (dates/get-current-year-string (cmd/get-years))
            {sid :_id} (cmd/make-student "test")
            {sid2 :_id} (cmd/make-student "test2")]
        (cmd/add-student-to-class sid class-id)
        (add-3good-2short-swipes sid)
        (cmd/swipe-in sid2 _2014_10_19)
        (cmd/swipe-in sid2 _2014_10_20)

        (let [att (db/get-report today-str class-id)
              student1 (first (filter #(= sid (:_id %)) att))
              student2 (first (filter #(= sid2 (:_id %)) att))]

          (student-report-is student1 3 2 0 0 0 25M)
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
  (do (sample-db)
      (let [today-str (dates/get-current-year-string (cmd/get-years))
            class-id (get-class-id-by-name "2014-2015")
            {sid :_id} (cmd/make-student "test")
            {sid2 :_id} (cmd/make-student "test2")]
        ;; good today
        (add-3good-2short-swipes sid)
        (cmd/override-date sid "2014-10-18")
        (cmd/excuse-date sid "2014-10-20")

        (cmd/swipe-in sid2 _2014_10_19)
        (cmd/swipe-in sid2 _2014_10_20)

        (cmd/add-student-to-class sid class-id)
        (cmd/add-student-to-class sid2 class-id)

        (let [att (db/get-report today-str)
              student1 (first (filter #(= sid (:_id %)) att))
              student2 (first (filter #(= sid2 (:_id %)) att))]
          (student-report-is student1 4 1 1 1 1 26M)

          (testing "Total short count student 2"
            (is (= (:short student2) 2)))
          (testing "Total Abs Count For Student 2 Should equal number of total days for student 1 and 2"
            (is (= (:unexcused student2)
                   5)))
          )
        (testing "an older date string shows no attendance in that time"
          (is (= '() (db/get-report "06-01-2013 05-01-2014")))))) 
  )

