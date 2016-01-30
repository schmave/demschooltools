(ns overseer.pfsqueries
  (:require [clojure.tools.trace :as trace]
            [overseer.migrations :as m]
            [yesql.core :refer [defqueries] ]))

(defqueries "overseer/phillyfreeschool.sql" )

(defmethod m/query "phillyfreeschool" [this]
  (trace/trace this)
  (let [f (resolve (symbol (str "overseer.pfsqueries/" (:q this))))]
    (trace/trace f)
    (trace/trace (:conn this))
    (f (:params this) (:conn this))))
