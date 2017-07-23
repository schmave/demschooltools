(ns overseer.web-test
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.pprint :as pp]
            [clojure.test :refer :all]
            [overseer.attendance :as att]
            [overseer.commands :as cmd]
            [overseer.queries :as queries]
            [overseer.database.users :as users]
            [overseer.database.connection :as conn]
            [overseer.dates :as dates]
            [overseer.db :as db]
            [overseer.helpers-test :refer :all]
            [schema.test :as tests]))

(comment
  (run-tests 'overseer.web-test)
  )

(deftest rounder
  (testing "rounding!"
    (let [_1am_ninth (dates/round-swipe-time (t/date-time 2015 10 9 5))]
      (is (= 9 (t/hour _1am_ninth)))
      (is (= 9 (t/day _1am_ninth)))
      )
    (let [_10pm_ninth (dates/round-swipe-time (t/date-time 2015 10 10 1))]
      (is (= 16 (t/hour _10pm_ninth)))
      (is (= 9 (t/day _10pm_ninth))))))

(defn get-school-days [year]
  (map :days (filter :days (queries/get-school-days year))))
;; (get-school-days "2014-06-01 2015-06-01")

(deftest get-school-days-test
  (sample-db true)
  (let [year (dates/get-current-year-string (queries/get-years))
        school-days (get-school-days year)]
    (testing "School days"
      (is (= school-days [(dates/make-date-string (t/minus (t/now)
                                                           (t/days 2)))
                          (dates/make-date-string (t/minus (t/now)
                                                           (t/days 1)))])))))

(deftest make-swipe-out
  (testing "sanitize"
    (testing "sanitize swipe out no times"
      (let [result (cmd/sanitize-out (cmd/make-swipe 1))]
        (is (= (-> result :in_time) nil))
        (is (= (-> result :out_time) nil))))
    (testing "sanitize swipe out with valid times does nothing"
      (let [passed (assoc (cmd/make-swipe 1)
                          :in_time (c/to-sql-time _2014_10-14_9-14am)
                          :out_time (c/to-sql-time (t/plus _2014_10-14_9-14am (t/minutes 5))))
            result (cmd/sanitize-out passed)]
        (is (= passed result))))
    (testing "sanitize swipe out with newer out time forces same out as in"
      (let [passed (assoc (cmd/make-swipe 1)
                          :in_time (c/to-sql-time _2014_10-14_9-14am)
                          :out_time (c/to-sql-time (t/minus _2014_10-14_9-14am (t/minutes 5)))
                          :rounded_in_time (c/to-sql-time _2014_10-14_9-14am)
                          :rounded_out_time (c/to-sql-time (t/minus _2014_10-14_9-14am (t/minutes 5))))
            result (cmd/sanitize-out passed)]
        (is (= (:in_time passed) (:in_time result)))
        (is (= (:in_time passed) (:out_time result)))
        (is (= (:rounded_in_time passed) (:rounded_out_time result)))
        (is (= (:rounded_in_time passed) (:rounded_in_time result)))
        ))
    (testing "sanitize swipe out with out in wrong day forces out to be same day"
      (let [passed (assoc (cmd/make-swipe 1)
                          :in_time (c/to-sql-time _2014_10-14_9-14am)
                          :out_time (c/to-sql-time (t/plus _2014_10-14_9-14am (t/minutes 5) (t/days 1))))
            result (cmd/sanitize-out passed)]
        (is (= (:in_time passed) (:in_time result)))
        (is (= (:in_time passed) (:out_time result)))))
    )

  )

(deftest no-student-name-invalid
  (testing "Email is set"
    (is (= nil (cmd/make-student "  ")))))

(deftest set-student-email
  (sample-db true)
  (let [{sid :_id date :start_date} (cmd/make-student "test")
        email "test@email.com"]
    (cmd/edit-student sid "test" date email)
    (let [s (-> (queries/get-students sid) first)]
      (testing "Email is set"
        (is (= email (:guardian_email s)))))))

(deftest student-start-date-prevents-short-count-test
  (do (sample-db true)
      (let [{sid :_id} (cmd/make-student "test")
            today (dates/today-string)]
        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/set-student-start-date sid today)
        (let [s (-> (queries/get-students sid) first)
              att (get-att sid)]
          (testing "Start date is set"
            (is (= today (str (:start_date s)))))
          (student-att-is att 0 0 0 0)
          ))))

(deftest swipe-attendence-override-test
  (do (sample-db)
      (let [{sid :_id} (cmd/make-student "test")]
        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid  _2014_10-14_9-14am)
        (cmd/swipe-out sid  (t/plus _2014_10-14_9-14am (t/hours 4)))
        (cmd/override-date sid "2014-10-14")
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
  (do (sample-db)
      (let [class-id (get-class-id-by-name "2014-2015")
            {student-id :_id} (cmd/make-student "Jimmy Hotel")]
        (cmd/activate-class class-id)
        (let [classes (queries/get-classes)
              students (queries/get-students-for-class class-id)]
          (testing "class creation"
            (is (= 1 (count classes)))
            (is (= true (-> classes first :active))))
          (testing "students in class"
            (is (= 3 (count students)))
            (is (= 2 (count (filter (comp not nil? :class_id) students))))
            )
          ))))

(defn _801pm []
  (today-at-utc 20 1))

(deftest swipe-in-and-out-at-8pm-with-rounding-test
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)]
        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid (_801pm))
        (cmd/swipe-out sid (t/plus (_801pm) (t/minutes 5)))
        (let [att (get-att sid)
              _10-14 (-> att :days first)
              first-swipe (-> _10-14 :swipes first)]
          (testing "swipe info"
            (is (= "08:06:00" (:nice_out_time first-swipe)))
            (is (= "08:01:00" (:nice_in_time first-swipe))))
          (testing "att stuff"
            (is (= (:total_mins _10-14) 0M))
            (is (= (:total_days att) 0))
            (is (= true (:in_today att))))
          ))))

(def _840am_2014_10_14 (t/minus _2014_10-14_9-14am (t/minutes 90)))
(def _339pm_2014_10_14 (t/plus _2014_10-14_9-14am (t/minutes 330)))

(def _900am_2014_10_14 (t/date-time 2014 10 14 13 0))
(def _500pm_2014_10_14 (t/date-time 2014 10 14 21 0))
(def _330pm_2014_10_14 (t/date-time 2014 10 14 19 30))

(comment 
 (deftest making-class-with-swipe-out-of-range
   (do (sample-db)
       (let [class (cmd/make-class "name" "2015-08-12"  "2016-06-10")
             cid (get-class-id-by-name "name")
             s (cmd/make-student "test")
             sid (:_id s)]
         (cmd/add-student-to-class sid cid)
         (cmd/activate-class cid)

         ;; swipe in 2014
         (cmd/swipe-in sid _900am_2014_10_14)
         (cmd/swipe-out sid _339pm_2014_10_14)
         (let [att (get-att sid)]
           (testing "Total Valid Day Count"
             (is (= (:total_days att)
                    0)))
           (testing "Total Minute Count"
             (is (= (-> att :days first :total_mins)
                    0M)))
           )))))

(deftest swipe-before-9-test
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        ;; only count minutes from 9-4
        (cmd/swipe-in sid _840am_2014_10_14)
        (cmd/swipe-out sid _339pm_2014_10_14)
        (let [att (get-att sid)]
          (testing "Total Valid Day Count"
            (is (= (:total_days att)
                   1)))
          (testing "Total Minute Count"
            (is (= (-> att :days first :total_mins)
                   399M)))

          ))))


(deftest swipe-roundings-after-4
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))

        ;; round after 4 back to 4
        (cmd/swipe-in sid (t/plus _900am_2014_10_14 (t/hours 3)))
        (cmd/swipe-out sid _500pm_2014_10_14)

        ;; round before 9 up to 9
        (cmd/swipe-in sid (t/plus _840am_2014_10_14 (t/days 1)))
        (cmd/swipe-out sid (t/plus _330pm_2014_10_14 (t/days 1)))

        (let [att (get-att sid)
              _10-15 (-> att :days first)
              _10-14 (-> att :days second)]
          (testing "10/15 has no out rounding"
            (is (= (-> _10-15 :swipes first :nice_out_time)
                   "03:30:00")))
          (testing "10/15 has in rounding"
            (is (= (-> _10-15 :swipes first :nice_in_time)
                   "08:39:27")))
          (testing "10/15 minutes" (is (= (-> _10-15 :total_mins) 390M)))
          (testing "10/15 valid" (is (= (-> _10-15 :valid) true)))
          (testing "10/14 has out rounding"
            (is (= (-> _10-14 :swipes first :nice_out_time)
                   "05:00:00")))
          (testing "10/14 has no in rounding"
            (is (= (-> _10-14 :swipes first :nice_in_time)
                   "12:00:00")))
          (testing "10/14 minutes" (is (= (-> _10-14 :total_mins) 240M)))
          (testing "10/14 not valid" (is (= (-> _10-14 :valid) false)))
          (testing "One short" (is (= (:total_short att) 1)))
          (testing "One attendance" (is (= (:total_days att) 1)))
          ))))

(deftest calculates-interval-test
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)
            _900am_2014_10_14 (today-at-utc 9 0)
            _902am (today-at-utc 9 2)
            ]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid _900am_2014_10_14)
        (cmd/swipe-out sid _902am)
        (let [att (get-att sid)]
          (testing "Interval Time"
            (is (= (-> att :days first :total_mins) 2M)))))))

(deftest single-swipe-short-test
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid _2014_10-14_9-14am)
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

(deftest schema-isolation-test
  (do (conn/init-pg)
      (users/reset-db)
      ;; no need to create any students in demo, they are already there
      (let [resp (make-sample-two-students-in-class)
            att  (att/get-student-list)]
        (testing "Student Count"
          (is (= 2 (count att)))))))

;; 10-14-2014 - good
;; 10-15-2014 - good
;; 10-16-2014 - short
;; 10-17-2014 - good
;; 10-18-2014 - short
;; 10-19-2014 - absent
;; 10-20-2014 - absent
(deftest swipe-attendence-test
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)
            s2 (cmd/make-student "test2")
            sid2 (:_id s2)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/add-student-to-class sid2 (get-class-id-by-name "2014-2015"))

        (add-3good-2short-swipes sid)

        ;; student not added to year doesn't show up
        (cmd/make-student "test3")

        (cmd/override-date sid "2014-10-18")
        (cmd/excuse-date sid "2014-10-20")

        ;; 10/19
        (cmd/swipe-in sid2 _2014_10_19)
        ;; 10/20
        (cmd/swipe-in sid2 _2014_10_20)

        (testing "getting classes"
          (let [cls (first (queries/get-classes))]
            (is (= 4 (-> cls :students count)))))

        (testing "School year is list of days with swipes"
          (is (= (get-school-days (dates/get-current-year-string (queries/get-years)))
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
                   26.5M)))
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
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (let [att (get-att sid)]
          (testing "in today is false"
            (is (= false (:in_today att)))))
        (cmd/swipe-in sid (t/now))
        (let [att (get-att sid)]
          (testing "in today is true"
            (is (= true (:in_today att)))))))
  )

(deftest swiped-in-yesterday-no-exception-test
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid  (t/minus (t/today-at 9 0 0) (t/days 1)))
        (cmd/swipe-out 3  (f/unparse (f/formatters :date-time)
                                      (t/minus (t/today-at 19 0 0) (t/days 1))))
        )))

(deftest swiped-out-without-in-no-exception-test
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)]
        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        )))

(deftest sign-out-without-in
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)]
        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid _2014_10-14_9-14am)
        (cmd/swipe-out sid (t/plus _2014_10-14_9-14am (t/days 1) (t/minutes 331)))
        (let [att (get-att sid)]
          (testing "swiping out on another day just is an out"
            (is (= 2 (-> att :days count))))))))

(deftest students-with-att
  (do (sample-db)
      (testing "students with att"
        (is (not= '() (att/get-student-with-att 1))))
      (let [s (cmd/make-student "test")
            sid (:_id s)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid _2014_10-14_9-14am)
        (cmd/swipe-out sid (t/plus _2014_10-14_9-14am (t/minutes 331)))

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

(deftest student-list-in-out-in
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)
            _8pm (today-at-utc 20 0)]
        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid (t/plus _8pm (t/seconds 1)))
        (cmd/swipe-out sid (t/plus _8pm (t/seconds 2)))
        (cmd/swipe-in sid (t/plus _8pm (t/seconds 3)))

        (let [att (att/get-student-list)
              our-hero (filter #(= sid (:_id %)) att)]
          (testing "Student Count"
            (is (= (->> our-hero count) 1))
            (is (= (->> our-hero first :last_swipe_type) "in"))
            (is (= (->> our-hero first :in_today) true))
            )))))

(deftest student-list-when-last-swipe-sanitized2
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)
            _3pm (today-at-utc 15 0)]
        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        ;; has an in, then missed two days, next in should
        ;; show a last swipe type
        (cmd/swipe-in 1 (t/minus _3pm (t/days 2)))
        (cmd/swipe-in 1 (t/minus _3pm (t/days 1)))
        (cmd/swipe-in sid (t/minus _3pm (t/days 3)))
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
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)
            _3pm (today-at-utc 15 0)]
        (cmd/swipe-in sid (t/minus _3pm (t/hours 1)))
        (cmd/swipe-out sid _3pm)
        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (let [att  (att/get-student-list)
              our-hero (filter #(= sid (:_id %)) att)]
          (testing "Student Count"
            (is (= (->> our-hero count) 1))
            (is (= (->> our-hero first :last_swipe_type) "out"))
            (is (= (->> our-hero first :in_today) true))
            )))))

(deftest absent-student-main-page
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)
            x (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
            s (cmd/toggle-student-absent sid)
            att (att/get-student-list)]

        (testing "Student Count"
          (is (= (-> att count) 3))
          (is (= (->> att (filter :absent_today) count) 1))
          (is (= (->> att (filter (complement :in_today)) count) 3))
          ))))

(deftest older-student-required-minutes
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)
            s (cmd/toggle-student-older sid)
            s (first (queries/get-students sid))
            tomorrow (-> (today-at-utc 10 0) (t/plus (t/days 1)))
            day-after-next (-> (today-at-utc 10 0) (t/plus (t/days 2)))
            ]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid tomorrow)
        (cmd/swipe-out sid (t/plus tomorrow (t/minutes 331)))

        (cmd/swipe-in sid (t/plus tomorrow (t/days 2)))
        (cmd/swipe-out sid (t/plus tomorrow (t/days 2) (t/minutes 329)))

        (let [att (get-att sid)]
          ;; (pp/pprint att)
          (testing "Total Valid Day Count"
            (is (= 1 (-> att :total_days)))))))
  )

(comment
  (deftest get-current-year
    (sample-db)
    (testing "Getting current year"
      (is (= (dates/get-current-year-string (queries/get-years))
             (str "2014-06-01 " (dates/today-string)))))))

(deftest excuse-today-is-today
  (do (sample-db)
      (let [
            {dummy-id :_id} (cmd/make-student "dummy")
            {sid :_id} (cmd/make-student "test")
            today-string (dates/today-string)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/add-student-to-class dummy-id (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in dummy-id)
        (cmd/excuse-date sid today-string)
        (let [att  (get-att sid)
              today (-> att :days first)]
          (testing "First day is today string"
            (is (= (:day today) today-string)))
          (testing "Today is excused"
            (is (= (:excused today) true)))
          )))
  )

(deftest swipe-today-not-in-on-excused-or-override
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)
            s2 (cmd/make-student "tests1")
            sid2 (:_id s2)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/override-date sid (dates/today-string))
        (cmd/excuse-date sid2 (dates/today-string))
        (let [att (get-att sid)
              att2 (get-att sid2)]
          (testing "S1 not in today"
            (is (= (:in_today att) false)))
          (testing "S2 not in today"
            (is (= (:in_today att2) false)))
          )))
  )

(deftest swipe-attendence-shows-only-when-in
  (do (sample-db)
      (let [s (cmd/make-student "test")
            sid (:_id s)]

        (cmd/add-student-to-class sid (get-class-id-by-name "2014-2015"))
        (cmd/swipe-in sid _2014_10-14_9-14am)
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
