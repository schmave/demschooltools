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

#_(deftest ^:performance massive-timing
  (sh/sh "make" "load-massive-dump")
  (let [x (time (att/get-student-with-att 1))
        stu (first x)]
    (testing "days all came back" (is (= 199 (count (:days stu)))))))

