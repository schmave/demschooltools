(ns overseer.helpers
  (:require [clojure.tools.trace :as trace]
            [clojure.tools.logging :as log]
            ))

;; (deftrace tester [a b] (+ a b))
;;(tester 1 2)

(def ^{:doc "Current stack depth of traced function calls." :private true :dynamic true}
  *trace-depth* 0)

(defn ^{:private true} tracer
  "This function is called by trace. Prints to standard output, but
may be rebound to do anything you like. 'name' is optional."
  [name value]
  (str "TRACE" (when name (str " " name)) ": " value))

(defn ^{:private true} trace-indent
  "Returns an indentation string based on *trace-depth*"
  []
  (apply str (take *trace-depth* (repeat "| "))))

(defn ^{:skip-wiki true} trace-fn-call
  "Traces a single call to a function f with args. 'name' is the
  symbol name of the function."
  [name f args]
  (let [id (gensym "t")
        fn-log (tracer id (str (trace-indent) (pr-str (cons name args))))]
    (let [value (binding [*trace-depth* (inc *trace-depth*)]
                  (apply f args))
          output-log (tracer id (str (trace-indent) "=> " (pr-str value))) ]
      (log/info (str "\n" fn-log "\n" output-log) )
      value)))

;; Borrowed from the clojure.trace library, because I want it
;; to use logging instead of trace
(defmacro deftrace
  "Use in place of defn; traces each call/return of this fn, including
   arguments. Nested calls to deftrace'd functions will print a
   tree-like structure.
   The first argument of the form definition can be a doc string"
  [name & definition]
  (let [doc-string (if (string? (first definition)) (first definition) "")
        fn-form  (if (string? (first definition)) (rest definition) definition)]
    `(do
       (declare ~name)
       (let [f# (fn ~@fn-form)]
         (defn ~name ~doc-string [& args#]
           (trace-fn-call '~name f# args#))))))




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
