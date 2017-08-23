(ns overseer.migrations
  (:require [environ.core :refer [env]]
            [migratus.core :as migratus]
            [clojure.java.jdbc :as jdbc]
            [heroku-database-url-to-jdbc.core :as h]
            [overseer.database.connection :refer [pgdb init-pg]]
            [clojure.java.io :as io]))

(defn create-dst-tables []
  (let [dst-migration (slurp "resources/migrations/dst_migration.sql")]
    (jdbc/execute! @pgdb [dst-migration])))

(defn migrate-db [con]
  (migratus/migrate {:store :database
                     :db con})
  (create-dst-tables)
  )

;; (migratus/create {:store :database :db @pgdb} "add is teacher")

(comment
  (migratus/migrate {:store :database
                     :db @pgdb}))
