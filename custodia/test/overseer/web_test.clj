(ns overseer.web-test
  (:require [clojure.test :refer :all]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.trace :as trace]
            [overseer.database :as db]
            [overseer.attendance :as att]
            [overseer.helpers :as h]
            [overseer.dates :as dates]
            ))
;; run test C-c M-,
;; run tests C-c ,
(comment
  (run-tests 'overseer.web-test)  
  )  

(def basetime (t/date-time 2014 10 14 14 9 27 246)) 

(defn get-att [id student]
  (let [year (dates/get-current-year-string (db/get-years))
        school-days (att/get-school-days year)]
    (att/get-attendance school-days year id student)))

(deftest get-school-days
  (db/sample-db true)  
  (let [year (dates/get-current-year-string (db/get-years))
        school-days (att/get-school-days year)]
    (testing "School days"
      (is (= school-days [(dates/make-date-string (t/minus (t/now)
                                                           (t/days 2)))
                          (dates/make-date-string (t/minus (t/now)
                                                           (t/days 1)))])))))

(defn add-swipes [sid]
  ;; 14 hours in UTC is 9 Am here
  ;; 10-14-2014 
  (db/swipe-in sid basetime)
  (db/swipe-out sid (t/plus basetime (t/hours 6)))

  ;; good tomorrow
  ;; 10-15-2014 

  (db/swipe-in sid (t/plus basetime (t/days 1)))
  (db/swipe-out sid (t/plus basetime (t/days 1) (t/hours 6)))

  ;; short the next
  ;; 10-16-2014 

  (db/swipe-in sid (t/plus basetime (t/days 2)))
  (db/swipe-out sid (t/plus basetime (t/days 2) (t/hours 4)))

  ;; two short the next but long enough
  ;; 10-17-2014 

  (db/swipe-in sid (t/plus basetime (t/days 3)))
  (db/swipe-out sid (t/plus basetime (t/days 3) (t/hours 4)))
  (db/swipe-in sid (t/plus basetime (t/days 3) (t/hours 5)))
  (db/swipe-out sid (t/plus basetime (t/days 3) (t/hours 7)))

  ;; short the next - 10-18-2014 
  (db/swipe-in sid (t/plus basetime (t/days 4)))
  (db/swipe-out sid (t/plus basetime (t/days 4) (t/hours 4)))
  )

(deftest make-swipe-out
  (testing "sanitize"
    (testing "sanitize swipe out no times"
      (let [result (db/sanitize-out (db/make-swipe 1))]
        (is (= (-> result :in_time) nil))
        (is (= (-> result :out_time) nil)))) 
    (testing "sanitize swipe out with valid times does nothing"
      (let [passed (assoc (db/make-swipe 1)
                     :in_time (str basetime)
                     :out_time (str (t/plus basetime (t/minutes 5))))
            result (db/sanitize-out passed)]
        (is (= passed result)))) 
    (testing "sanitize swipe out with newer out time forces same out as in"
      (let [passed (assoc (db/make-swipe 1)
                     :in_time (str basetime)
                     :out_time (str (t/minus basetime (t/minutes 5))))
            result (db/sanitize-out passed)]
        (is (= (:in_time passed) (:in_time result)))
        (is (= (:in_time passed) (:out_time result)))
        ))
    (testing "sanitize swipe out with out in wrong day forces out to be same day"
      (let [passed (assoc (db/make-swipe 1)
                     :in_time (str basetime)
                     :out_time (str (t/plus basetime (t/minutes 5) (t/days 1))))
            result (db/sanitize-out passed)]
        (is (= (:in_time passed) (:in_time result)))
        (is (= (:in_time passed) (:out_time result)))))
    )   

  )

(deftest swipe-attendence-override-test
  (do (db/sample-db)  
      (let [s (db/make-student "test")
            sid (:_id s)]
        (db/swipe-in sid (str basetime))
        (db/swipe-out sid (str (t/plus basetime (t/hours 4))))
        (db/override-date sid "2014-10-14")
        (let [att (get-att sid s)]
          (testing "Total Valid Day Count"
            (is (= (:total_days att)
                   1)))
          (testing "Total Abs Count"
            (is (= (:total_abs att)
                   0)))
          (testing "Total Hours"
            (is (= (:total_hours att)
                   5)))
          (testing "Override"
            (is (= (-> att :days first :override)
                   true)))
          ))) 
  )

(deftest single-swipe-short-test
  (do (db/sample-db)  
      (let [s (db/make-student "test")
            sid (:_id s)]
        (db/swipe-in sid basetime)
        (let [att (get-att sid s)]
          (testing "Total Valid Day Count"
            (is (= (:total_days att)
                   0)))
          (testing "Total Short Day Count"
            (is (= (:total_short att)
                   1)))
          (testing "Total Abs Count"
            (is (= (:total_abs att)
                   0)))
          (testing "Total Overrides"
            (is (= (:total_overrides att)
                   0)))
          ))))


;; 10-14-2014 - good
;; 10-15-2014 - good
;; 10-16-2014 - short
;; 10-17-2014 - good
;; 10-18-2014 - short
;; 10-19-2014 - absent
;; 10-20-2014 - absent
(deftest swipe-attendence-test
  (do (db/sample-db)  
      (let [s (db/make-student "test")
            sid (:_id s) 
            s2 (db/make-student "test2")
            sid2 (:_id s2)]
        ;; good today
        (add-swipes sid)
        (db/override-date sid "2014-10-18")
        (db/excuse-date sid "2014-10-20")
        (db/swipe-in sid2 (t/plus basetime (t/days 5)))
        (db/swipe-in sid2 (t/plus basetime (t/days 6)))

        (testing "School year is list of days with swipes"
          (is (= (att/get-school-days "2014-06-01 2015-06-01")
                 (list "2014-10-14" "2014-10-15" "2014-10-16" "2014-10-17" "2014-10-18" "2014-10-19" "2014-10-20"))))
        (testing "Student Front Page Count"
          (is (= (-> (att/get-student-list) count) 4)))
        (let [att (get-att sid s)
              att2 (get-att sid2 s2)]
          (testing "Total Valid Day Count"
            (is (= (:total_days  att)
                   4)))
          (testing "Total Short Day Count"
            (is (= (:total_short att)
                   1)))
          (testing "Total Excused Count"
            (is (= (:total_excused att)
                   1)))
          (testing "Total Abs Count"
            (is (= (:total_abs att)
                   1)))
          (testing "Total Overrides"
            (is (= (:total_overrides att)
                   1)))
          (testing "Total Hours"
            (is (= (:total_hours att)
                   27.0)))
          (testing "Days sorted correctly"
            (is (= (-> att :days first :day)
                   "2014-10-20")))
          (testing "Nice time shown correctly"
            (is (= (-> att :days (nth 2) :swipes first :nice_in_time)
                   ;; shown as hour 10 because that was DST forward +1
                   "10:09:27")))
          (testing "Total short count student 2"
            (is (= (:total_short att2)
                   2)))
          (testing "Total Abs Count For Student 2 Should equal number of total days for student 1 and 2"
            (is (= (:total_abs att2)
                   5)))
          )
        (testing "an older date string shows no attendance in that time"
          (let [att (att/get-attendance [] "06-01-2013 05-01-2014" sid s)]
            (testing "Total Valid Day Count"
              (is (= (:total_days att)
                     0)))
            (testing "Total Abs Count"
              (is (= (:total_abs att)
                     0)))
            (testing "Total Overrides"
              (is (= (:total_overrides att)
                     0)))
            )))) 
  )

(deftest sign-out-without-in
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)]
        (db/swipe-in sid basetime)
        (db/swipe-out sid (t/plus basetime (t/days 1) (t/minutes 331)))
        (let [att (get-att sid s)]
          (testing "swiping out on another day just is an out"
            (is (= 2 (-> att :days count)))))))
  )

(deftest students-with-att
  (do (db/sample-db)
      (testing "students with att"
        (is (not= '() (att/get-students-with-att))))
      (let [s (db/make-student "test")
            sid (:_id s)]
        (db/swipe-in sid basetime)
        (db/swipe-out sid (t/plus basetime (t/minutes 331)))

        (let [att  (att/get-students-with-att
                    (dates/get-current-year-string (db/get-years))
                    sid)]
          (testing "has one valid"
            (is (=  (-> att first :total_days) 1 )))
          (testing "students with att doesn't throw exceptions"
            (is (not= '() att))))))
  )

(deftest older-student-required-minutes
  (db/sample-db)
  (let [s (db/make-student "test")
        sid (:_id s)
        s (db/toggle-student-absent sid)
        att (trace/trace "att" (att/get-student-list))]
    (testing "Student Count"
      (is (= (-> att count) 3))
      (is (= (->> att (filter :absent_today) count) 1))
      (is (= (->> att (filter (complement :in_today)) count) 3))
      )))

(deftest older-student-required-minutes
  (db/sample-db)
  (let [s (db/make-student "test")
        sid (:_id s)
        s (db/toggle-student-older sid)
        s (first (db/get-students sid))
        tomorrow (-> (t/today-at 8 0) (t/plus (t/days 1)))
        day-after-next (-> (t/today-at 8 0) (t/plus (t/days 2)))
        ]
    (db/swipe-in sid tomorrow)
    (db/swipe-out sid (t/plus tomorrow (t/minutes 331)))

    (db/swipe-in sid (t/plus tomorrow (t/days 2)))
    (db/swipe-out sid (t/plus tomorrow (t/days 2) (t/minutes 329)))

    (let [att (get-att sid s)]
      (testing "Total Valid Day Count"
        (is (= (-> att :total_days) 1)))))
  )

(deftest get-current-year
  (db/sample-db)
  (testing "Getting current year"
    (is (= (dates/get-current-year-string (db/get-years))
           "2014-06-01 2015-06-01"))))

(deftest excuse-today-is-today
  (do (db/sample-db)  
      (let [s (db/make-student "test")
            sid (:_id s)
            today-string (dates/today-string)]
        (db/excuse-date sid today-string)
        (let [att  (get-att sid s)
              today (-> att :days first)]
          (testing "First day is today string"
            (is (= (:day today) today-string)))
          (testing "Today is excused"
            (is (= (:excused today) true)))
          ))) 
  )

(deftest swipe-today-not-in-on-excused-or-override
  (do (db/sample-db)  
      (let [s (db/make-student "test")
            sid (:_id s)
            s2 (db/make-student "tests1")
            sid2 (:_id s2)]
        (db/override-date sid (dates/today-string))
        (db/excuse-date sid2 (dates/today-string))
        (let [att (get-att sid s)
              att2 (get-att sid2 s2)]
          (testing "S1 not in today"
            (is (= (:in_today att) false)))
          (testing "S2 not in today"
            (is (= (:in_today att2) false)))
          ))) 
  )

(deftest swipe-attendence-shows-only-when-in
  (do (db/sample-db)  
      (let [s (db/make-student "test")
            sid (:_id s)]
        (db/swipe-in sid basetime)
        (let [att (get-att sid s)]
          (testing "Total Valid Day Count"
            (is (= (-> att :days first :day)
                   "2014-10-14")))
          (testing "Last Swipe was an 'in'"
            (is (= (-> att :last_swipe_type)
                   "in")))
          ))) 
  )
