(ns overseer.queries.phillyfreeschool
  (:require [clojure.tools.trace :as trace]
            [overseer.migrations :as m]
            [yesql.core :refer [defqueries] ]))

(defqueries "overseer/queries/phillyfreeschool.sql" )
