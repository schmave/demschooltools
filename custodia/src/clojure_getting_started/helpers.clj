(ns clojure-getting-started.helpers
  (:require [clojure.tools.trace :as trace]))

(defn wrap-args-with-trace [[symb val]]
  [symb (list clojure.tools.trace/trace (str "let-" symb) val)])

(defmacro tracelet [args & body]
  (let [arg-pairs (partition 2 args)
        new-bindings (vec (mapcat wrap-args-with-trace arg-pairs))]
    `(let ~new-bindings ~@body)))

(defn ?assoc
  "Same as assoc, but skip the assoc if v is nil"
  [m & kvs]
  (->> kvs
       (partition 2)
       (filter second)
       (map vec)
       (into m)))
