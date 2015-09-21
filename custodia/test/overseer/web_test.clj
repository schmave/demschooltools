(ns overseer.web-test
  (:require [clojure.test :refer :all]
            [overseer.helpers-test :refer :all]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.trace :as trace]
            [overseer.database :as data]
            [overseer.db :as db]
            [overseer.attendance :as att]
            [overseer.dates :as dates]
            [schema.test :as tests]
            ))

;; run test C-c M-,
;; run tests C-c ,
(comment
  (run-tests 'overseer.web-test)
  )

(defn get-school-days [year]
  (map :days (filter :days (db/get-school-days year))))
;; (get-school-days "2014-06-01 2015-06-01")

(deftest rounder
  (testing "rounding!"
    (let [_1am_ninth (dates/round-swipe-time (t/date-time 2015 10 9 5))]
      (is (= 9 (t/hour _1am_ninth)))
      (is (= 9 (t/day _1am_ninth)))
      )
    (let [_10pm_ninth (dates/round-swipe-time (t/date-time 2015 10 10 1))]
      (is (= 16 (t/hour _10pm_ninth)))
      (is (= 9 (t/day _10pm_ninth)))
      )))

(deftest get-school-days-test
  (data/sample-db true)
  (let [year (dates/get-current-year-string (data/get-years))
        school-days (get-school-days year)]
    (testing "School days"
      (is (= school-days [(dates/make-date-string (t/minus (t/now)
                                                           (t/days 2)))
                          (dates/make-date-string (t/minus (t/now)
                                                           (t/days 1)))])))))

(deftest make-swipe-out
  (testing "sanitize"
    (testing "sanitize swipe out no times"
      (let [result (data/sanitize-out (data/make-swipe 1))]
        (is (= (-> result :in_time) nil))
        (is (= (-> result :out_time) nil))))
    (testing "sanitize swipe out with valid times does nothing"
      (let [passed (assoc (data/make-swipe 1)
                          :in_time (c/to-sql-time _10-14_9-14am)
                          :out_time (c/to-sql-time (t/plus _10-14_9-14am (t/minutes 5))))
            result (data/sanitize-out passed)]
        (is (= passed result))))
    (testing "sanitize swipe out with newer out time forces same out as in"
      (let [passed (assoc (data/make-swipe 1)
                          :in_time (c/to-sql-time _10-14_9-14am)
                          :out_time (c/to-sql-time (t/minus _10-14_9-14am (t/minutes 5))))
            result (data/sanitize-out passed)]
        (is (= (:in_time passed) (:in_time result)))
        (is (= (:in_time passed) (:out_time result)))
        ))
    (testing "sanitize swipe out with out in wrong day forces out to be same day"
      (let [passed (assoc (data/make-swipe 1)
                          :in_time (c/to-sql-time _10-14_9-14am)
                          :out_time (c/to-sql-time (t/plus _10-14_9-14am (t/minutes 5) (t/days 1))))
            result (data/sanitize-out passed)]
        (is (= (:in_time passed) (:in_time result)))
        (is (= (:in_time passed) (:out_time result)))))
    )

  )

(deftest swipe-attendence-override-test
  (do (data/sample-db)
      (let [{sid :_id} (data/make-student "test")]
        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/swipe-in sid  _10-14_9-14am)
        (data/swipe-out sid  (t/plus _10-14_9-14am (t/hours 4)))
        (data/override-date sid "2014-10-14")
        (let [att (get-att sid)]
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

(deftest class-and-adding-students
  (do (data/sample-db)
      (let [class-id (get-class-id-by-name "2014-2015")
            {student-id :_id} (data/make-student "Jimmy Hotel")]
        (db/activate-class class-id)
        (let [classes (db/get-classes)
              students (db/get-students-for-class class-id)]
          (testing "class creation"
            (is (= 1 (count classes)))
            (is (= true (-> classes first :active))))
          (trace/trace students)
          (testing "students in class"
            (is (= 3 (count students)))
            (is (= 2 (count (filter (comp not nil? :class_id) students))))
            )
          ))))

(deftest archive-student
  (do (data/sample-db)
      (testing "starts 2" (is (= 2 (count (att/get-student-list false)))))
      (let [s (data/make-student "test")
            sid (:_id s)]
        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/toggle-student-archived sid)
        (testing "Archived missing" (is (= 2 (count (att/get-student-list false)))))
        (testing "Archived missing" (is (= 3 (count (att/get-student-list true))))))))

(def _801pm (today-at-utc 20 1))

(deftest swipe-in-and-out-at-8pm-with-rounding-test
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/swipe-in sid _801pm)
        (data/swipe-out sid (t/plus _801pm (t/minutes 5)))
        (let [att (get-att sid)
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

(def _840am (t/minus _10-14_9-14am (t/minutes 90)))
(def _339pm (t/plus _10-14_9-14am (t/minutes 330)))

(deftest swipe-before-9-test
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        ;; only count minutes from 9-4
        (data/swipe-in sid _840am)
        (data/swipe-out sid _339pm)
        (let [att (get-att sid)]
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
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))

        ;; round after 4 back to 4
        (data/swipe-in sid (t/plus _900am (t/hours 3)))
        (data/swipe-out sid _500pm)

        ;; round before 9 up to 9
        (data/swipe-in sid (t/plus _840am (t/days 1)))
        (data/swipe-out sid (t/plus _330pm (t/days 1)))

        (let [att (get-att sid)
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
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/swipe-in sid _10-14_9-14am)
        (let [att (get-att sid)]
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
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)
            s2 (data/make-student "test2")
            sid2 (:_id s2)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/add-student-to-class sid2 (get-class-id-by-name "2014-2015"))

        (add-3good-2short-swipes sid)

        ;; student not added to year doesn't show up
        (data/make-student "test3")

        (data/override-date sid "2014-10-18")
        (data/excuse-date sid "2014-10-20")

        ;; 10/19
        (data/swipe-in sid2 _10-19)
        ;; 10/20
        (data/swipe-in sid2 _10-20)

        (testing "getting classes"
          (let [cls (first (db/get-classes))]
            (is (= 4 (-> cls :students count)))))

        (testing "School year is list of days with swipes"
          (is (= (get-school-days (dates/get-current-year-string (data/get-years)))
                 (list "2014-10-14" "2014-10-15" "2014-10-16" "2014-10-17" "2014-10-18" "2014-10-19" "2014-10-20"))))
        (testing "Student Front Page Count"
          (is (= (-> (att/get-student-list) count) 4)))
        (let [att (get-att sid)
              att2 (get-att sid2)]
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
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (let [att (get-att sid)]
          (testing "in today is false"
            (is (= false (:in_today att)))))
        (data/swipe-in sid (t/now))
        (let [att (get-att sid)]
          (testing "in today is true"
            (is (= true (:in_today att)))))))
  )

(deftest sign-out-without-in
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/swipe-in sid _10-14_9-14am)
        (data/swipe-out sid (t/plus _10-14_9-14am (t/days 1) (t/minutes 331)))
        (let [att (get-att sid)]
          (testing "swiping out on another day just is an out"
            (is (= 2 (-> att :days count)))))))
  )

(deftest students-with-att
  (do (data/sample-db)
      (testing "students with att"
        (is (not= '() (att/get-student-with-att 1))))
      (let [s (data/make-student "test")
            sid (:_id s)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/swipe-in sid _10-14_9-14am)
        (data/swipe-out sid (t/plus _10-14_9-14am (t/minutes 331)))

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
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)
            now (today-at-utc 15 0)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        ;; has an in, then missed two days, next in should
        ;; show a last swipe type
        (data/swipe-in 1 (t/minus now (t/days 2)))
        (data/swipe-in 1 (t/minus now (t/days 1)))
        (data/swipe-in sid (t/minus now (t/days 3)))
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
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)
            now (today-at-utc 15 0)]
        (data/swipe-in sid (t/plus now (t/hours 1)))
        (data/swipe-out sid now)
        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (let [att  (att/get-student-list)
              our-hero (filter #(= sid (:_id %)) att)]
          (trace/trace our-hero)
          (testing "Student Count"
            (is (= (->> our-hero count) 1))
            (is (= (->> our-hero first :last_swipe_type) "out"))
            (is (= (->> our-hero first :in_today) true))
            )))))

(deftest absent-student-main-page
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)
            x (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
            s (data/toggle-student-absent sid)
            att (trace/trace "att" (att/get-student-list))]

        (testing "Student Count"
          (is (= (-> att count) 3))
          (is (= (->> att (filter :absent_today) count) 1))
          (is (= (->> att (filter (complement :in_today)) count) 3))
          ))))

(deftest older-student-required-minutes
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)
            s (data/toggle-student-older sid)
            s (first (data/get-students sid))
            tomorrow (-> (today-at-utc 9 0) (t/plus (t/days 1)))
            day-after-next (-> (today-at-utc 9 0) (t/plus (t/days 2)))
            ]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/swipe-in sid tomorrow)
        (data/swipe-out sid (t/plus tomorrow (t/minutes 331)))

        (data/swipe-in sid (t/plus tomorrow (t/days 2)))
        (data/swipe-out sid (t/plus tomorrow (t/days 2) (t/minutes 329)))

        (let [att (get-att sid)]
          (testing "Total Valid Day Count"
            (is (= (-> att :total_days) 1))))))
  )

(comment
 (deftest get-current-year
   (data/sample-db)
   (testing "Getting current year"
       (is (= (dates/get-current-year-string (data/get-years))
              (str "2014-06-01 " (dates/today-string)))))))

(deftest excuse-today-is-today
  (do (data/sample-db)
      (let [dummy (data/make-student "dummy")
            s (data/make-student "test")
            sid (:_id s)
            today-string (dates/today-string)]
        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/add-student-to-class (:_id dummy) (get-class-id-by-name "2014-2015"))
        (data/swipe-in (:_id dummy))
        (data/excuse-date sid today-string)
        (let [att  (get-att sid)
              today (-> att :days first)]
          (testing "First day is today string"
            (is (= (:day today) today-string)))
          (testing "Today is excused"
            (is (= (:excused today) true)))
          )))
  )

(deftest swipe-today-not-in-on-excused-or-override
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)
            s2 (data/make-student "tests1")
            sid2 (:_id s2)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/override-date sid (dates/today-string))
        (data/excuse-date sid2 (dates/today-string))
        (let [att (get-att sid)
              att2 (get-att sid2)]
          (testing "S1 not in today"
            (is (= (:in_today att) false)))
          (testing "S2 not in today"
            (is (= (:in_today att2) false)))
          )))
  )

(deftest swipe-attendence-shows-only-when-in
  (do (data/sample-db)
      (let [s (data/make-student "test")
            sid (:_id s)]

        (data/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (data/swipe-in sid _10-14_9-14am)
        (let [att (get-att sid)]
          (testing "Total Valid Day Count"
            (is (= (-> att :days first :day)
                   "2014-10-14")))
          (testing "Last Swipe was an 'in'"
            (is (= (-> att :last_swipe_type)
                   "in")))
          )))
  )

(use-fixtures :once schema.test/validate-schemas)
