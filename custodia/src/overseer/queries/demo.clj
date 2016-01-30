(ns overseer.queries.demo
  (:require [clojure.tools.trace :as trace]
            [overseer.migrations :as m]
            [yesql.core :refer [defqueries] ]))

(def a (m/make-queries "demo"))

(defqueries "overseer/queries/demo.sql" )
