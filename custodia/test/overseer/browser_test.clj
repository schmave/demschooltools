(ns overseer.browser-test
  (:require [clojure.test :refer :all]
            [clojure.java.shell :as sh]
            [clj-time.core :as t]
            [overseer.helpers-test :refer :all]
            [clj-webdriver.taxi :refer :all]))

(defn student-id [id] (str "#student-" id))

(defn click-student [id]
  ;;(wait-until #(visible? (student-id id)) 1000)
  ;;(click (student-id id))
  (to (str "http://localhost:5000/#/students/" id "/"))
  ;;(wait-until #(visible? "#studentName") 10000)
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

(defn go-to-class-page []
  (clickw "#class-link"))

(defn go-to-totals-page []
  (clickw "#totals-link"))

(defn create-class [name]
  (go-to-class-page)
  (clickw "#create-class")
  (input-text "#Class" name)
  (clickw "#create-class-button")
  (clickw (str "#" name)))

(defn create-student [name]
  (clickw "#create-student")
  (input-text "#studentName" name)
  (clickw "#saveStudent"))

(defn add-to-class [student-id]
  (clickw (str "#add-" student-id)))

(defn ensure-class-selected [name]
  (testing "class selected"
    (is (= name
           (text (first (selected-options "#class-select"))))))
  )

(defn activate-class [name]
  (clickw (str "#activate-" name)))

(defn login-to-site []
  (do (set-driver! {:browser :firefox} "http://localhost:5000/users/login")
      (login)))

(defn assert-total-header [att abs ex over short id]
  (testing (str "student page totals: " id)
    (wait-until #(visible? "#hd-attended"))
    (is (= (str "Attended: " (+ short att) " (" short ")") (text "#hd-attended")))
    (is (= (str "Unexcused: " abs) (text "#hd-absent")))
    (is (= (str "Excused: " ex) (text "#hd-excused")))
    (is (= (str "Override: " over) (text "#hd-given")))
   ;; (is (= (str "Short: " short) (text "#hd-short")))
    ))

(deftest ^:integration make-classes-and-set-default
  (sample-db)
  (login-to-site)

  (create-class "test")

  (add-to-class 1)

  (activate-class "test")

  (go-to-totals-page)
  (ensure-class-selected "test")

  (quit))

(deftest ^:integration edit-name
  (sample-db)
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
  (sample-db)
  (login-to-site)

  (data/swipe-in 1 (t/minus (t/now) (t/days 1)))
  (assert-student-in-not-in-col 1)
  (click-student 1)
  (sign-in)
  (clickw "#submit-missing")

  (assert-student-in-in-col 1)

  (quit))

(deftest ^:integration overrides-and-excuses
  (sample-db)
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

(deftest ^:integration first-swipe-isnt-missing-front-page
  (do
    (sh/sh "make" "load-aliased-dump")
    (login-to-site))

  (create-student "new")
  (clickw "#home")

  (go-to-class-page)
  (add-to-class 57)

  (clickw "#home")
  (sign-in-front-page 57)

  (click-student 57)

  (assert-total-header 0 15 0 0 1 "")
  (quit))

(deftest ^:integration missing-swipe-twice-front-page
  (sample-db true)
  (login-to-site)

  (assert-student-in-not-in-col 1)
  (assert-student-in-not-in-col 2)

  (click-student 1)
  (assert-total-header 0 1 0 0 1 "1b")
  (clickw "#home")

  (click-student 2)
  (assert-total-header 0 1 0 0 1 "2b")
  (clickw "#home")

  (sign-in-front-page 1)
  (submit-missing)
  (click-student 1)
  (assert-total-header 1 1 0 0 1 "1a")
  (clickw "#home")

  (sign-in-front-page 2)
  (submit-missing)

  (clickw "#home")
  (click-student 2)
  (assert-total-header 1 1 0 0 1 "2a")
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
