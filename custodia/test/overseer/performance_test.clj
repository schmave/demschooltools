(ns overseer.performance-test
  (:require [clojure.test :refer :all]
            [clj-time.format :as f]
            [clojure.java.shell :as sh]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.trace :as trace]
            [overseer.db :as d]
            [overseer.attendance :as att]
            [overseer.helpers :as h]
            [overseer.dates :as dates]
            [clojure.pprint :refer :all]
            ))

;; (pprint (d/get-student-page 1 "2014-06-01 2015-06-01"))
;; (count (d/get-student-page 1 "2014-06-01 2015-06-01"))

(deftest ^:performance massive-timing
  (sh/sh "make" "load-massive-dump")
  (let [x (time (att/get-students-with-att))
        stu (trace/trace "stud" (first x))]
    (testing "" (is (= 80 (count x))))))

;; (def x (time (do (trace/trace "Total") (att/get-student-attendence-this-year 1))))
;; (d/get-student-page 1 "2014-06-01 2015-06-01")

