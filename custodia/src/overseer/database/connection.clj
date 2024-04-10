(ns overseer.database.connection
  (:require [heroku-database-url-to-jdbc.core :as h]
            [environ.core :refer [env]]
            ))

(def pgdb (atom nil))

;; (init-pg) 
(defn init-pg []
  (swap! pgdb (fn [old]
                (dissoc (h/korma-connection-map (env :database-url))
                        :classname))))
