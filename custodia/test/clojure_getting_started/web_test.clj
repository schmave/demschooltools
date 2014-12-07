(ns clojure-getting-started.web-test
  (:require [clojure.test :refer :all]
            [clojure-getting-started.web :refer :all]))

(comment
  (run-tests 'clojure-getting-started.web-test)  
  )

(run-tests 'clojure-getting-started.web-test)  

(deftest first-test
  (sample-db)
  (make-student "steve")
  (swipe-in 1)
  (swipe-out 1)
  (let [swipes (get-swipes 1)]
    (is 1 (count swipes))
    (is true (not= nil (:swipe-out (first swipes)))))
  )

#_(with-redefs [clj-time.core/now (fn [] "adf")]
    (clj-time.core/now))
