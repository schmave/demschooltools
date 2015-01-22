(ns clojure-getting-started.web-test
  (:require [clojure.test :refer :all]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.trace :as trace]
            [clojure-getting-started.database :as db]
            [clojure-getting-started.attendance :as att]
            [clojure-getting-started.dates :as dates]
            ))
;; run test C-c M-,
;; run tests C-c ,
(comment
  (run-tests 'clojure-getting-started.web-test)  
  )  

(def basetime (t/date-time 2014 10 14 14 9 27 246)) 

(defn get-att [id student]
  (let [year (dates/get-current-year-string (db/get-years))
        school-days (att/get-school-days year)]
    (att/get-attendance school-days year id student)))

(deftest date-stuff
  (= (dates/make-date-string "2014-12-28T14:32:12.509Z")
     "12-28-2014")
  ;; this will fail after DST *shakes fist*
  (is (= (dates/make-time-string "2014-12-28T14:32:12.509Z")
         "09:32:12")))

(defn add-swipes [sid]
  ;; 14 hours in UTC is 9 Am here
  (db/swipe-in sid basetime)
  (db/swipe-out sid (t/plus basetime (t/hours 6)))

  ;; good tomorrow
  
  (db/swipe-in sid (t/plus basetime (t/days 1)))
  (db/swipe-out sid (t/plus basetime (t/days 1) (t/hours 6)))

  ;; short the next
  
  (db/swipe-in sid (t/plus basetime (t/days 2)))
  (db/swipe-out sid (t/plus basetime (t/days 2) (t/hours 4)))


  ;; two short the next but long enough
  
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
  (db/sample-db)  
  (let [s (db/make-student "test")
        sid (:_id s)]
    (db/swipe-in sid basetime)
    (db/swipe-out sid (t/plus basetime (t/hours 4)))
    (db/override-date sid "2014-10-14")
    (let [att (get-att sid s)]
      (testing "Total Valid Day Count"
        (is (= (:total_days att)
               1)))
      (testing "Total Abs Count"
        (is (= (:total_abs att)
               0)))
      (testing "Override"
        (is (= (-> att :days first :override)
               true)))
      )) 
  )

(deftest swipe-attendence-test
  (do (db/sample-db)  
      (let [
            s (db/make-student "test")
            sid (:_id s) 
            s2 (db/make-student "test2")
            sid2 (:_id s2)]
        ;; good today
        (add-swipes sid)
        (db/override-date sid "2014-10-18")

        (testing "School year is list of days with swipes"
          (is (= (att/get-school-days "2014-06-01 2015-06-01")
                 (list "2014-10-14" "2014-10-15" "2014-10-16" "2014-10-17" "2014-10-18"))))
        (let [att (get-att sid s)
              att2 (get-att sid2 s2)]
          (testing "Total Valid Day Count"
            (is (= (:total_days att)
                   4)))
          (testing "Total Abs Count"
            (is (= (:total_abs att)
                   1)))
          (testing "Total Overrides"
            (is (= (:total_overrides att)
                   1)))
          (testing "Days sorted correctly"
            (is (= (-> att :days first :day)
                   "2014-10-18")))
          (testing "Nice time shown correctly"
            (is (= (-> att :days first :swipes first :nice_in_time)
                   ;; shown as hour 10 because that was DST forward +1
                   "10:09:27")))
          (testing "Total Abs Count For Student 2 Should equal number of total days for student 1"
            (is (= (:total_abs att2)
                   5)))
          )
        (testing "an older date string shows no attendance in that time"
          (let [att (att/get-attendance [] "06-01-2013-05-01-2014" sid s)]
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

(deftest older-student-required-minutes
  (db/sample-db)
  (let [s (db/make-student "test")
        sid (:_id s)
        s (db/toggle-student sid)
        s (first (db/get-students sid))

        tomorrow (-> (t/today-at 8 0) (t/plus (t/days 1)))]
    ;; good today
    ;;(let [basetime (t/date-time 2014 10 14 14 9 27 246)])

    (db/swipe-in sid tomorrow)
    (db/swipe-out sid (t/plus tomorrow (t/minutes 331)))

    (db/swipe-in sid (t/plus tomorrow (t/days 2)))
    (db/swipe-out sid (t/plus tomorrow (t/days 2) (t/minutes 329)))

    (let [att (get-att sid s)]
      (testing "Total Valid Day Count"
        (is (= (-> att :total_days) 1)))))
  )


(deftest swipe-attendence-shows-only-when-in
  (do (db/sample-db)  
      (let [s (db/make-student "test")
            sid (:_id s)]
        ;; good today
        ;;(let [basetime (t/date-time 2014 10 14 14 9 27 246)])
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

(comment
  {:_id "58c6cbea991a928e4c7a66848603da45", :_rev "1-1f2fed784543664c7f72e3196267bacf", :name "test2", :last_swipe_type nil, :today "2015-01-21", :days ({:valid true, :override true, :day "2014-10-18", :total_mins 240, :swipes [{:_id "58c6cbea991a928e4c7a668486040aa4", :_rev "2-67cf053b7af3c80128e75c7adf6ba116", :nice_out_time "02:09:27", :type "swipe", :inserted-date "2015-01-21T23:46:01.920Z", :nice_in_time "10:09:27", :interval 240, :student_id "58c6cbea991a928e4c7a66848603cad9", :out_time "2014-10-18T18:09:27.246Z", :in_time "2014-10-18T14:09:27.246Z"} {:_id "58c6cbea991a928e4c7a668486040d7d", :_rev "1-aadd9ab431da2a9e1aaf0b1adc3473cc", :inserted-date "2015-01-21T23:46:01.968Z", :type "override", :student_id "58c6cbea991a928e4c7a66848603cad9", :date "2014-10-18"}]} {:valid true, :override false, :day "2014-10-17", :total_mins 360, :swipes [{:_id "58c6cbea991a928e4c7a66848603f62a", :_rev "2-5e27806476b04bfb8887978f0646d036", :nice_out_time "02:09:27", :type "swipe", :inserted-date "2015-01-21T23:46:01.718Z", :nice_in_time "10:09:27", :interval 240, :student_id "58c6cbea991a928e4c7a66848603cad9", :out_time "2014-10-17T18:09:27.246Z", :in_time "2014-10-17T14:09:27.246Z"} {:_id "58c6cbea991a928e4c7a66848603fd95", :_rev "2-ce3f156142a3ec247a2dc91314e65e5c", :nice_out_time "05:09:27", :type "swipe", :inserted-date "2015-01-21T23:46:01.812Z", :nice_in_time "03:09:27", :interval 120, :student_id "58c6cbea991a928e4c7a66848603cad9", :out_time "2014-10-17T21:09:27.246Z", :in_time "2014-10-17T19:09:27.246Z"}]} {:valid false, :override false, :day "2014-10-16", :total_mins 240, :swipes [{:_id "58c6cbea991a928e4c7a66848603f33e", :_rev "2-a87c43cd2b1aaee0138c7a557e5c5a84", :nice_out_time "02:09:27", :type "swipe", :inserted-date "2015-01-21T23:46:01.646Z", :nice_in_time "10:09:27", :interval 240, :student_id "58c6cbea991a928e4c7a66848603cad9", :out_time "2014-10-16T18:09:27.246Z", :in_time "2014-10-16T14:09:27.246Z"}]} {:valid true, :override false, :day "2014-10-15", :total_mins 360, :swipes [{:_id "58c6cbea991a928e4c7a66848603e60e", :_rev "2-222bbc141edd14fb47eb05b92f9c49a8", :nice_out_time "04:09:27", :type "swipe", :inserted-date "2015-01-21T23:46:01.582Z", :nice_in_time "10:09:27", :interval 360, :student_id "58c6cbea991a928e4c7a66848603cad9", :out_time "2014-10-15T20:09:27.246Z", :in_time "2014-10-15T14:09:27.246Z"}]} {:valid true, :override false, :day "2014-10-14", :total_mins 360, :swipes [{:_id "58c6cbea991a928e4c7a66848603dc88", :_rev "2-6c0c32c3354c97157715dccb6d55657d", :nice_out_time "04:09:27", :type "swipe", :inserted-date "2015-01-21T23:46:01.508Z", :nice_in_time "10:09:27", :interval 360, :student_id "58c6cbea991a928e4c7a66848603cad9", :out_time "2014-10-14T20:09:27.246Z", :in_time "2014-10-14T14:09:27.246Z"}]}), :total_abs 1, :type :student, :inserted-date "2015-01-21T23:46:01.456Z", :total_overrides 1, :total_days 4, :olderdate nil, :last_swipe_date nil})


