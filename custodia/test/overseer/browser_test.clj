(ns overseer.browser-test
  (:require [clojure.test :refer :all]
            [clojure.java.shell :as sh]
            [clj-webdriver.taxi :refer :all]))

(defn click-student [id]
  (wait-until #(visible? (str "a#student-" id)))
  (click (str "a#student-" id))
  (wait-until #(visible? "#studenttotalrow")))

(defn submit-missing []
  (wait-until #(visible? "#submit-missing"))
  (click "#submit-missing"))

(defn clickw [q]
  (wait-until #(visible? q))
  (click q))

(defn login []
  (wait-until #(visible? "#username"))
  (input-text "#username" "admin")
  (input-text "#password" "changeme")
  (submit "#password"))

(deftest ^:integration absent-column
  (sh/sh "make" "load-aliased-dump")
  ;;(set-driver! {:browser :firefox} "http://shining-overseer-test.herokuapp.com/login")
  (set-driver! {:browser :firefox} "http://localhost:5000/login")
  (login)
  (click-student 7)

  (wait-until #(visible? "#confirmed-absent"))
  (click "#sign-in")
  ;; (submit-missing)
  (click-student 7)
  (testing "student page totals"
    (is (= (text "#studenttotalrow")
           "Attended: 3 - Absent: 4 - Excused: 7 - Overrides: 0 - Short: 6")))
  (quit))

(deftest ^:integration loggin-in
  (sh/sh "make" "load-aliased-dump")
  ;;(set-driver! {:browser :firefox} "http://shining-overseer-test.herokuapp.com/login")
  (set-driver! {:browser :firefox} "http://localhost:5000/login")
  (login)
  (click-student 7)
  (testing "student page totals"
    (is (= (text "#studenttotalrow")
           "Attended: 3 - Absent: 4 - Excused: 7 - Overrides: 0 - Short: 5")))

  (wait-until #(visible? "#sign-in"))
  (click "#sign-in")
  ;; (submit-missing)
  (click-student 7)
  (testing "student page totals"
    (is (= (text "#studenttotalrow")
           "Attended: 3 - Absent: 4 - Excused: 7 - Overrides: 0 - Short: 6")))
  (quit))
