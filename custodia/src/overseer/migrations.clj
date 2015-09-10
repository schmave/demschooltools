(ns overseer.migrations
  (:require [migratus.core :as migratus]
            [environ.core :refer [env]]
            [heroku-database-url-to-jdbc.core :as h]))

(defn mconfig []
  {:store                :database
   :migration-dir        "migrations2"
   :migration-table-name "swipes"
   :db {:connection-uri "jdbc:postgresql://localhost:5432/swipes?user=postgres&password=changeme"}
   })

(defn mconfig []
  {:store                :database
   :migration-dir        "migrations2"
   :migration-table-name "swipes"
   :db {:classname "org.postgresql.Driver",
        :subprotocol "postgresql",
        :user "postgres",
        :password "changeme",
        :subname "//localhost:5432/swipes"}
   })


;; (migratus/create (mconfig) "create-school-year")
;; (migratus/migrate (mconfig))  
