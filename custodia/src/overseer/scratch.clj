(ns overseer.scratch)

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
