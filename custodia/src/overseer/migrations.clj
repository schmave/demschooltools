(ns overseer.migrations
  (:require [environ.core :refer [env]]
            [migratus.core :as migratus]
            [heroku-database-url-to-jdbc.core :as h]
            [overseer.database.connection :refer [pgdb init-pg]]
            [clojure.java.io :as io]))

(defn replace-philly [from with]
  (clojure.string/replace from #"phillyfreeschool" with))

(defn make-queries [name]
  (let [data (slurp "src/overseer/queries/phillyfreeschool.sql")
        data (replace-philly data name)]
    (with-open [wrtr (io/writer (str "src/overseer/queries/generated-" name ".sql"))]
      (.write wrtr data))))

(defn migrate-db [con]
  (migratus/migrate {:store :database
                     :db con}))

;; (migratus/create {:store :database :db @pgdb} "calculated interval")

(comment
  (migratus/migrate {:store :database
                     :db {}}))
