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
            [schema.test :as tests]
            ))

(defn today-at-utc [h m]
  (t/plus (t/today-at h m) (t/hours 4)))


;; run test C-c M-,
;; run tests C-c ,
(comment
  (run-tests 'overseer.web-test)
  )

(def basetime (t/date-time 2014 10 14 14 9 27 246))

(defn get-att [id student]
  (let [year (dates/get-current-year-string (db/get-years))]
    (att/get-attendance year id student)))

(deftest get-school-days
  (db/sample-db true)
  (let [year (dates/get-current-year-string (db/get-years))
        school-days (att/get-school-days year)]
    (testing "School days"
      (is (= school-days [(dates/make-date-string (t/minus (t/now)
                                                           (t/days 2)))
                          (dates/make-date-string (t/minus (t/now)
                                                           (t/days 1)))])))))

;; (t/date-time 2015 4 14 4 3 27 456)

(defn add-swipes [sid]
  ;; 14 hours in UTC is 9 Am here
  ;; 10-14-2014
  (db/swipe-in sid basetime)
  (db/swipe-out sid (t/plus basetime (t/hours 6)))

  ;; good tomorrow
  ;; 10-15-2014
  (db/swipe-in sid (t/plus basetime (t/days 1)))
  (db/swipe-out sid (t/plus basetime (t/days 1) (t/hours 6)))

  (comment (do (db/sample-db)
               (let [s (db/make-student "test")
                     sid (:_id s)]
                 (db/swipe-in sid (t/plus basetime (t/days 1)))
                 (db/swipe-out sid (t/plus basetime (t/days 1) (t/hours 6)))
                 (att/get-student-with-att sid)

                 )))

  ;; short the next
  ;; 10-16-2014

  (db/swipe-in sid (t/plus basetime (t/days 2)))
  (db/swipe-out sid (t/plus basetime (t/days 2) (t/hours 4)))

  ;; two short the next but long enough
  ;; 10-17-2014

  (db/swipe-in sid (t/plus basetime (t/days 3)))
  (db/swipe-out sid (t/plus basetime (t/days 3) (t/hours 4)))
  (db/swipe-in sid (t/plus basetime (t/days 3) (t/hours 4.1)))
  (db/swipe-out sid (t/plus basetime (t/days 3) (t/hours 6.1)))

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
                          :in_time (c/to-sql-time basetime)
                          :out_time (c/to-sql-time (t/plus basetime (t/minutes 5))))
            result (db/sanitize-out passed)]
        (is (= passed result))))
    (testing "sanitize swipe out with newer out time forces same out as in"
      (let [passed (assoc (db/make-swipe 1)
                          :in_time (c/to-sql-time basetime)
                          :out_time (c/to-sql-time (t/minus basetime (t/minutes 5))))
            result (db/sanitize-out passed)]
        (is (= (:in_time passed) (:in_time result)))
        (is (= (:in_time passed) (:out_time result)))
        ))
    (testing "sanitize swipe out with out in wrong day forces out to be same day"
      (let [passed (assoc (db/make-swipe 1)
                          :in_time (c/to-sql-time basetime)
                          :out_time (c/to-sql-time (t/plus basetime (t/minutes 5) (t/days 1))))
            result (db/sanitize-out passed)]
        (is (= (:in_time passed) (:in_time result)))
        (is (= (:in_time passed) (:out_time result)))))
    )

  )

(deftest swipe-attendence-override-test
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)]
        (db/swipe-in sid  basetime)
        (db/swipe-out sid  (t/plus basetime (t/hours 4)))
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

(deftest archive-student
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)]
        (db/toggle-student-archived sid)
        (let [list (att/get-student-list)]
          (testing "Archived missing"
            (is (= 0 (count list)))
            )))))

(def _801pm (today-at-utc 20 1))

(deftest swipe-in-and-out-at-8pm-with-rounding-test
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)]
        (db/swipe-in sid _801pm)
        (db/swipe-out sid (t/plus _801pm (t/minutes 5)))
        (let [att (get-att sid s)
              _10-14 (-> att :days first)
              first-swipe (-> _10-14 :swipes first)]
          (testing "swipe info"
            (is (= "04:00:00" (:nice_out_time first-swipe)))
            (is (= "04:00:00" (:nice_in_time first-swipe))))

          (testing "att stuff"
            (is (= (:total_mins _10-14) 0.0))
            (is (= (:total_days att) 0))
            (is (= true (:in_today att))))

          ))))

(def _840am (t/minus basetime (t/minutes 90)))
(def _339pm (t/plus basetime (t/minutes 330)))

(deftest swipe-before-9-test
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)]
        ;; only count minutes from 9-4
        (db/swipe-in sid _840am)
        (db/swipe-out sid _339pm)
        (let [att (get-att sid s)]
          (testing "Total Valid Day Count"
            (is (= (:total_days att)
                   1)))
          (testing "Total Minute Count"
            (is (= (-> att :days first :total_mins)
                   399.4541)))

          ))))

(def _900am (t/date-time 2014 10 14 13 0))
(def _500pm (t/date-time 2014 10 14 21 0))
(def _330pm (t/date-time 2014 10 14 19 30))

(deftest swipe-roundings-after-4
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)]

        ;; round after 4 back to 4
        (db/swipe-in sid (t/plus _900am (t/hours 3)))
        (db/swipe-out sid _500pm)

        ;; round before 9 up to 9
        (db/swipe-in sid (t/plus _840am (t/days 1)))
        (db/swipe-out sid (t/plus _330pm (t/days 1)))

        (let [att (get-att sid s)
              _10-15 (-> att :days first)
              _10-14 (-> att :days second)]
          (trace/trace att)
          (testing "10/15 has no out rounding"
            (is (= (-> _10-15 :swipes first :nice_out_time)
                   "03:30:00")))
          (testing "10/15 has in rounding"
            (is (= (-> _10-15 :swipes first :nice_in_time)
                   "09:00:00")))
          (testing "10/15 minutes" (is (= (-> _10-15 :total_mins) 390.0)))
          (testing "10/15 valid" (is (= (-> _10-15 :valid) true)))
          (testing "10/14 has out rounding"
            (is (= (-> _10-14 :swipes first :nice_out_time)
                   "04:00:00")))
          (testing "10/14 has no in rounding"
            (is (= (-> _10-14 :swipes first :nice_in_time)
                   "12:00:00")))
          (testing "10/14 minutes" (is (= (-> _10-14 :total_mins) 240.0)))
          (testing "10/14 not valid" (is (= (-> _10-14 :valid) false)))
          (testing "One short" (is (= (:total_short att) 1)))
          (testing "One attendance" (is (= (:total_days att) 1)))
          ))))

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

        ;; 10/19
        (db/swipe-in sid2 (t/plus basetime (t/days 5)))
        ;; 10/20
        (db/swipe-in sid2 (t/plus basetime (t/days 6)))

        (testing "School year is list of days with swipes"
          (is (= (att/get-school-days (dates/get-current-year-string (db/get-years)))
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
                   26.527295000000002)))
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
          (let [att (att/get-attendance "06-01-2013 05-01-2014" sid s)]
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

(deftest in-today-works
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)]
        (let [att (get-att sid s)]
          (testing "in today is false"
            (is (= false (:in_today att)))))
        (db/swipe-in sid (t/now))
        (let [att (get-att sid s)]
          (testing "in today is true"
            (is (= true (:in_today att)))))))
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
        (is (not= '() (att/get-student-with-att 1))))
      (let [s (db/make-student "test")
            sid (:_id s)]
        (db/swipe-in sid basetime)
        (db/swipe-out sid (t/plus basetime (t/minutes 331)))

        (let [att  (att/get-student-with-att sid)]
          (testing "has one valid"
            (is (=  (-> att first :total_days) 1 )))
          (testing "students with att doesn't throw exceptions"
            (is (not= '() att))))))
  )


(deftest student-list-when-last-swipe-sanitized1
  (let [res (att/get-last-swipe-type
             [{:swipes [{:day "2015-04-17", :nice_out_time nil, :type "", :nice_in_time nil, :has_override nil, :out_time nil, :in_time nil}]}
              {:swipes [{:day "2015-04-16",  :nice_out_time nil, :type "", :nice_in_time nil, :has_override nil, :out_time nil, :in_time nil}]}
              {:swipes [{:day "2015-04-15", :nice_out_time nil, :type "swipes", :nice_in_time "07:47:25", :out_time nil, :in_time "2015-04-15T11:47:25.516000000-00:00"}]}])]
    (testing "Can get last swipe type"
      (is (not= nil res)))))

(deftest student-list-when-last-swipe-sanitized2
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)
            now (today-at-utc 15 0)]
        ;; has an in, then missed two days, next in should
        ;; show a last swipe type
        (db/swipe-in 1 (t/minus now (t/days 2)))
        (db/swipe-in 1 (t/minus now (t/days 1)))
        (db/swipe-in sid (t/minus now (t/days 3)))
        (let [att (first (att/get-student-with-att sid))]
          (testing "Student Att Count"
            (is (= (->> att :last_swipe_type) "in"))
            (is (= (->> att :in_today) false))
            ))

        (let [att  (att/get-student-list)
              our-hero (filter #(= sid (:_id %)) att)]
          (testing "Student Count"
            (is (= (->> our-hero count) 1))
            (is (= (->> our-hero first :last_swipe_type) "in"))
            (is (= (->> our-hero first :in_today) false))
            )))))

(deftest student-list-when-last-swipe-sanitized3
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)
            now (today-at-utc 15 0)]
        (db/swipe-in sid (t/plus now (t/hours 1)))
        (db/swipe-out sid now)
        (let [att  (att/get-student-list)
              our-hero (filter #(= sid (:_id %)) att)]
          (trace/trace our-hero)
          (testing "Student Count"
            (is (= (->> our-hero count) 1))
            (is (= (->> our-hero first :last_swipe_type) "out"))
            (is (= (->> our-hero first :in_today) true))
            )))))

(deftest absent-student-main-page
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
  (do (db/sample-db)
      (let [s (db/make-student "test")
            sid (:_id s)
            s (db/toggle-student-older sid)
            s (first (db/get-students sid))
            tomorrow (-> (today-at-utc 9 0) (t/plus (t/days 1)))
            day-after-next (-> (today-at-utc 9 0) (t/plus (t/days 2)))
            ]
        (db/swipe-in sid tomorrow)
        (db/swipe-out sid (t/plus tomorrow (t/minutes 331)))

        (db/swipe-in sid (t/plus tomorrow (t/days 2)))
        (db/swipe-out sid (t/plus tomorrow (t/days 2) (t/minutes 329)))

        (let [att (get-att sid s)]
          (testing "Total Valid Day Count"
            (is (= (-> att :total_days) 1))))))
  )

(comment
 (deftest get-current-year
   (db/sample-db)
   (testing "Getting current year"
       (is (= (dates/get-current-year-string (db/get-years))
              (str "2014-06-01 " (dates/today-string)))))))

(deftest excuse-today-is-today
  (do (db/sample-db)
      (let [
            dummy (db/make-student "dummy")
            s (db/make-student "test")
            sid (:_id s)
            today-string (dates/today-string)]
        (db/swipe-in (:_id dummy))
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

(use-fixtures :once schema.test/validate-schemas)
