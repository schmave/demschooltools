(ns overseer.scratch
  (:require [clojure.tools.trace :as t]))

(def d (fn [coll n]
         ((apply comp (cons first (repeat n rest))) coll)))

(def d
  (fn [coll n]
    (if (= 0 n)
      (first coll)
      (recur (rest coll) (dec n)))))

(comment (and
          (= (d '(4 5 6 7) 2) 6)
          (= (d [:a :b :c] 0) :a)
          (= (d [1 2 3 4] 1) 2)
          (= (d '([1 2] [3 4] [5 6]) 2) [5 6])))

(defn doer [coll]
  (->> (seq coll)
       (reduce (fn [[acc pred] e]
                 (if (pred e)
                   [(conj acc e) #(not= e %)]
                   [acc pred]))
                [[] identity])
       first))

(defn doer [coll]
  coll)

(fn [coll]
  (let [c (seq coll)]
    (first
     (reduce
      (fn s [[acc pred] e]
        (if (pred e)
          [(conj acc e) (fn [x] ((complement =) e x))]
          [acc pred]))
      [[] (fn [x] true)]
      c))))

(doer [1 1 2 3 1 4 4])
