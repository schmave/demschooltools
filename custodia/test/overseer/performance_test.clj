(ns overseer.performance-test
  (:require [clojure.test :refer :all]
            [clj-time.format :as f]
            [clojure.java.shell :as sh]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.trace :as trace]
            [overseer.database :as db]
            [overseer.attendance :as att]
            [overseer.helpers :as h]
            [overseer.dates :as dates]
            ))

(deftest ^:performance massive-timing
  (sh/sh "make" "load-massive-dump")
  (time (att/get-students-with-att)))
