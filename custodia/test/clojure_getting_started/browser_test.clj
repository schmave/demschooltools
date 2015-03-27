(ns clojure-getting-started.browser-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :refer :all]))

(deftest ^:integration loggin-in
  ;;(set-driver! {:browser :firefox} "http://shining-overseer-test.herokuapp.com/login")
  (set-driver! {:browser :firefox} "http://localhost:5000/login")

  ;; (click "a[href*='login']")
  (wait-until #(exists? "#username"))
  (input-text "#username" "admin")
  (input-text "#password" "changeme")
  (submit "#password")
  (wait-until #(exists? "a#student-7"))
  (click "a#student-7")
  (wait-until #(exists? "div#studenttotalrow"))
  (testing "student page totals"
    (is (= (value "div#studenttotalrow")
           (text "div#studenttotalrow")
           "Attended: 3")))
  (quit))
