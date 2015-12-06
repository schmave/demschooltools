(ns overseer.browser-test
  (:require [clojure.test :refer :all]
            [clojure.java.shell :as sh]
            [clj-time.core :as t]
            [overseer.database :as data]
            [clj-webdriver.taxi :refer :all]))

(defn student-id [id] (str "#student-" id))

(defn click-student [id]
  (wait-until #(visible? (student-id id)) 1000)
  (click (student-id id))
  #_(to (str "http://localhost:5000/#/students/" id "/"))
  #_(wait-until #(visible? "#studentName") 10000)
  )

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
  (wait-until #(visible? (str ".in " (student-id id)))))

(defn assert-student-in-abs-col [id]
  (wait-until #(visible? (str ".absent " (student-id id)))))

(defn assert-student-in-out-col [id]
  (wait-until #(visible? (str ".out " (student-id id)))))

(defn assert-student-in-not-in-col [id]
  (wait-until #(visible? (str ".not-in " (student-id id)))))

(defn sign-in [] (clickw "#sign-in"))
(defn sign-out [] (clickw "#sign-out"))
(defn override [] (clickw "#override"))
(defn sign-out-front-page [id] (clickw (str ".sign-" id)))
(defn sign-in-front-page [id] (clickw (str ".sign-" id)))

(defn login-to-site []
  (do (set-driver! {:browser :firefox} "http://localhost:5000/users/login")
      (login)))

(defn assert-total-header [att abs ex over short id]
  (testing (str "student page totals: " id)
    (wait-until #(visible? "#hd-attended"))
    (is (= (text "#hd-attended") (str "Attended: " att)))
    (is (= (text "#hd-absent") (str "Absent: " abs)))
    (is (= (text "#hd-excused") (str "Excused: " ex)))
    (is (= (text "#hd-given") (str "Gave Attendance: " over)))
    (is (= (text "#hd-short") (str "Short: " short))))
    )



(deftest ^:integration edit-name
  (data/sample-db)
  (login-to-site)
  ;; Edit then save name
  (click-student 1)

  (clickw "#edit-name")
  (clear "#studentName")
  (input-text "#studentName" "newname")
  (clickw "#save-name")

  (clickw "#home")
  (assert-student-in-not-in-col 1)
  (testing (is (= "newname" (text (student-id 1)))))

  ;; Edit then cancel name
  (click-student 1)
  (clickw "#edit-name")
  (clear "#studentName")
  (input-text "#studentName" "othername")
  (clickw "#cancel-name")

  (clickw "#home")
  (assert-student-in-not-in-col 1)
  (testing (is (= "newname" (text (student-id 1)))))

  (quit))

(deftest ^:integration filling-in-missing-swipes
  (data/sample-db)
  (login-to-site)

  (data/swipe-in 1 (t/minus (t/now) (t/days 1)))
  (assert-student-in-not-in-col 1)
  (click-student 1)
  (sign-in)
  (clickw "#submit-missing")

  (assert-student-in-in-col 1)

  (quit))

(deftest ^:integration overrides-and-excuses
  (data/sample-db)
  (login-to-site)

  (assert-student-in-not-in-col 1)
  (click-student 1)
  (sign-in)
  (assert-student-in-in-col 1)

  (click-student 2)
  (override)
  (Thread/sleep 200)
  (assert-total-header 1 0 0 1 0 "First")

  (clickw "#home")
  (assert-student-in-not-in-col 2)
  (click-student 1)
  (assert-total-header 0 0 0 0 1 "second")

  (sign-out)
  (assert-student-in-out-col 1)
  (click-student 1)

  (assert-total-header 0 0 0 0 1 "third")
  (quit))

;; Parousia
;; Custodia
;; Vigilia

(deftest ^:integration absent-column
  (sh/sh "make" "load-aliased-dump")
  (login-to-site)

  (assert-student-in-not-in-col 7)
  (click-student 7)
  (clickw "#absent-button")
  #_(accept)
  (clickw "#home")
  (assert-student-in-abs-col 7)

  (assert-student-in-not-in-col 8)
  (click-student 8)
  (sign-in)
  (submit-missing)
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

(deftest ^:integration missing-out-swipe
  (sh/sh "make" "load-aliased-dump")
  (login-to-site)

  (click-student 40)

  (assert-total-header 9 4 0 0 2 "")
  (sign-in)
  (submit-missing)

  (click-student 40)

  (assert-total-header 9 4 0 0 3 "")
  (quit))

(deftest ^:integration missing-out-swipe-from-front-page
  (sh/sh "make" "load-aliased-dump")
  (login-to-site)

  (click-student 40)

  (assert-total-header 9 4 0 0 2 "")

  (clickw "#home")
  (sign-in-front-page 40)
  (submit-missing)

  (click-student 40)

  (assert-total-header 9 4 0 0 3 "")
  (quit))

(deftest ^:integration missing-swipe-twice-front-page
  (data/sample-db)
  (login-to-site)

  (assert-student-in-not-in-col 1)
  (assert-student-in-not-in-col 2)

  (click-student 1)
  (assert-total-header 0 0 0 0 1 "1")
  (clickw "#home")

  (click-student 2)
  (assert-total-header 0 0 0 0 1 "2")
  (clickw "#home")

  (sign-in-front-page 1)
  (submit-missing)
  (click-student 1)
  (assert-total-header 1 0 0 0 1 "1")
  (clickw "#home")

  (sign-in-front-page 2)
  (submit-missing)
  (click-student 2)
  (assert-total-header 1 0 0 0 1 "1")
  (clickw "#home")

  (assert-student-in-in-col 1)
  (assert-student-in-in-col 2)

  (quit))

(deftest ^:integration loggin-in
  (sh/sh "make" "load-aliased-dump")
  (login-to-site)

  (click-student 7)
  (assert-total-header 3 5 7 0 5 "")

  (sign-in)
  (click-student 7)
  (assert-total-header 3 5 7 0 6 "")
  (quit))
