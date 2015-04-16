(ns overseer.browser-test
  (:require [clojure.test :refer :all]
            [clojure.java.shell :as sh]
            [clj-time.core :as t]
            [overseer.database :as data]
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

(defn assert-student-in-in-col [id]
  (wait-until #(visible? (str "#in-col a#student-" id))))

(defn assert-student-in-abs-col [id]
  (wait-until #(visible? (str "#absent-col a#student-" id))))

(defn assert-student-in-out-col [id]
  (wait-until #(visible? (str "#out-col a#student-" id))))

(defn assert-student-in-not-in-col [id]
  (wait-until #(visible? (str "#not-in-col a#student-" id))))

(defn sign-in [] (clickw "#sign-in"))
(defn sign-out [] (clickw "#sign-out"))

(deftest ^:integration filling-in-missing-swipes
  (do (data/sample-db)
      (set-driver! {:browser :firefox} "http://localhost:5000/login")
      (login))

  (data/swipe-in 1 (t/minus (t/now) (t/days 1)))
  (assert-student-in-not-in-col 1)
  (click-student 1)
  (sign-in)
  (clickw "#submit-missing")

  (assert-student-in-in-col 1)


  (quit))

(deftest ^:integration overrides-and-excuses
  (do (data/sample-db)
      (set-driver! {:browser :firefox} "http://localhost:5000/login")
      (login))

  (assert-student-in-not-in-col 1)
  (click-student 1)
  (sign-in)
  (assert-student-in-in-col 1)

  (click-student 2)
  (clickw ".override")
  (accept)
  (testing "student page totals"
    (is (= (text "#studenttotalrow")
           "Attended: 1 - Absent: 0 - Excused: 0 - Overrides: 1 - Short: 0")))

  (clickw "#back-main-page")
  (assert-student-in-not-in-col 2)

  (click-student 1)
  (testing "student page totals"
    (is (= (text "#studenttotalrow")
           "Attended: 0 - Absent: 0 - Excused: 0 - Overrides: 0 - Short: 1")))
  (sign-out)
  (assert-student-in-out-col 1)
  (click-student 1)

  (wait-until #(visible? ".override"))
  (testing "student page totals"
    (is (= (text "#studenttotalrow")
           "Attended: 0 - Absent: 0 - Excused: 0 - Overrides: 0 - Short: 1")))

  (quit))

;; Parousia
;; Custodia
;; Vigilia

(deftest ^:integration absent-column
  (do (sh/sh "make" "load-aliased-dump")
      (set-driver! {:browser :firefox} "http://localhost:5000/login")
      (login))

  (assert-student-in-not-in-col 7)
  (click-student 7)
  (clickw "#absent-button")
  (accept)
  (clickw "#back-main-page")
  (assert-student-in-abs-col 7)

  (assert-student-in-not-in-col 8)
  (click-student 8)
  (sign-in)
  (assert-student-in-in-col 8)
  (click-student 8)
  (sign-out)
  (assert-student-in-out-col 8)

  (assert-student-in-not-in-col 9)
  (click-student 9)
  (sign-out)
  (clickw "#submit-missing")
  (assert-student-in-out-col 9)

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

  (sign-in)
  ;; (submit-missing)
  (click-student 7)
  (testing "student page totals"
    (is (= (text "#studenttotalrow")
           "Attended: 3 - Absent: 4 - Excused: 7 - Overrides: 0 - Short: 6")))
  (quit))
