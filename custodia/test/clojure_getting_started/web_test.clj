(ns clojure-getting-started.web-test
  (:require [clojure.test :refer :all]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.trace :as trace]
            [clojure-getting-started.database :as db]))

(comment
  (run-tests 'clojure-getting-started.web-test)  
  )  

(deftest date-stuff
  (= (db/make-date-string "2014-12-28T14:32:12.509Z")
     "12-28-2014")
  ;; this will fail after DST *shakes fist*
  (is (= (db/make-time-string "2014-12-28T14:32:12.509Z")
         "09:32:12")))

(defn add-swipes [sid]
  ;; 14 hours in UTC is 9 Am here
  (let [basetime (t/date-time 2014 10 14 14 9 27 246)]
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
    (db/swipe-in sid (t/plus basetime (t/days 3)))
    (db/swipe-out sid (t/plus basetime (t/days 3) (t/hours 3)))))

(deftest swipe-attendence-test
  (db/sample-db)  
  (let [sid (-> "test" db/make-student :_id)]
    ;; good today
    (add-swipes sid)
    (let [att (db/get-attendance sid)]
      (testing "Total Valid Day Count"
        (is (= (:total_days att)
               3)))
      (testing "Total Abs Count"
        (is (= (:total_abs att)
               1)))
      (testing "Days sorted correctly"
        (is (= (-> att :days first :day)
               "10-14-2014")))
      (testing "Nice time shown correctly"
        (is (= (-> att :days first :swipes first :nice_in_time)
               "09:09:27")))
      )) 
  )

(comment {:total_days 2, :total_abs 1, :days ({:valid true, :day "10-14-2014", :total_mins 360, :swipes [{:nice_out_time "03:09:27", :nice_in_time "09:09:27", :interval 360, :_id "d152dcfff8282f3ffa590d8f9a00fb4e", :_rev "2-0c6538f07be3e825f457a6c77c086ca4", :out_time "2014-10-14T15:09:27.246Z", :type "swipe", :student_id "d152dcfff8282f3ffa590d8f9a00f951", :in_time "2014-10-14T09:09:27.246Z"}]} {:valid true, :day "10-15-2014", :total_mins 360, :swipes [{:nice_out_time "03:09:27", :nice_in_time "09:09:27", :interval 360, :_id "d152dcfff8282f3ffa590d8f9a010780", :_rev "2-9b40bea0ef22868cadba5fb23bc26d80", :out_time "2014-10-15T15:09:27.246Z", :type "swipe", :student_id "d152dcfff8282f3ffa590d8f9a00f951", :in_time "2014-10-15T09:09:27.246Z"}]} {:valid false, :day "10-16-2014", :total_mins 240, :swipes [{:nice_out_time "01:09:27", :nice_in_time "09:09:27", :interval 240, :_id "d152dcfff8282f3ffa590d8f9a011101", :_rev "2-b438d5cb24cb92e5a3b411df19aea4c0", :out_time "2014-10-16T13:09:27.246Z", :type "swipe", :student_id "d152dcfff8282f3ffa590d8f9a00f951", :in_time "2014-10-16T09:09:27.246Z"}]})}) 
