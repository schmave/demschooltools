(ns overseer.migrations
  (:require [environ.core :refer [env]]
            [migratus.core :as migratus]
            [clojure.java.jdbc :as jdbc]
            [heroku-database-url-to-jdbc.core :as h]
            [overseer.database.connection :refer [pgdb init-pg]]
            [clojure.java.io :as io]
            [overseer.helpers :as logh]))

(defn create-dst-tables
  ([] (create-dst-tables @pgdb))
  ([con]
   (let [dst-migration (slurp "resources/migrations/dst_migration.sql")]
     (jdbc/execute! con [dst-migration]))))

(defn migrate-db
  ([] (migrate-db @pgdb))
  ([con]
   (migratus/migrate {:store :database
                      :db con})))

;; (migratus/create {:store :database :db @pgdb} "add is teacher")

(comment
  (migratus/migrate {:store :database
                     :db @pgdb}))
