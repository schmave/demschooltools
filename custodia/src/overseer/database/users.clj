(ns overseer.database.users
  (:import [java.sql PreparedStatement]
           [java.util Date Calendar TimeZone])
  (:require [clojure.java.jdbc :as jdbc]
            [overseer.helpers :as logh]
            [overseer.database.connection :refer [pgdb]]

            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])

            [environ.core :refer [env]]
            [overseer.roles :as roles]

            [overseer.migrations :as migrations]
            ;; [overseer.queries.demo :as demo]
            [yesql.core :refer [defqueries]]
            [overseer.db :as db]

            ))

(defqueries "overseer/base.sql" )

;; (insert-email "test@test.com")
(logh/deftrace insert-email [email]
  (jdbc/insert! @pgdb :overseer.emails {:email email}))

;; (get-user "admin2")
(defn get-user [username]
  (println "get-user" username)
  (if-let [u (first (get-user-y { :username username} {:connection @pgdb}))]
    (assoc u :roles (read-string (:roles u)))))

;;(get-users)
(defn get-users []
  (->> (jdbc/query @pgdb ["select * from overseer.users;"])
       (map #(dissoc % :password))))

;;(set-user-schema "super" "TEST")
;;(get-user "super")
(defn set-user-school [_id school]
  (jdbc/update! @pgdb :users {:school_id school} ["user_id=?" _id]))

(defn make-new-school [school]
  (db/persist! {:type :schools :name school}))

(defn make-user
  ([username password roles]
   (make-user username password roles db/*school-id*))
  ([username password roles school]
   (if-not (get-user username)
     (jdbc/insert! @pgdb :overseer.users
                   {:username username
                    :password (creds/hash-bcrypt password)
                    :school_id school
                    :roles  (str (conj roles roles/user))}))))

(defn init-users []
  (make-user "admin" (env :adminpass) #{roles/admin roles/user} 1)
  (make-user "super" (env :adminpass) #{roles/admin roles/user roles/super} 1)
  (make-user "user" (env :userpass) #{roles/user} 1)
  (make-user "admin2" (env :adminpass) #{roles/admin roles/user} 2)
  (make-user "demo" (env :userpass) #{roles/admin roles/user} 2)
  )

(defn drop-all-tables []
  (jdbc/execute! @pgdb [(str "DROP SCHEMA IF EXISTS overseer CASCADE;"
                             "DROP TABLE IF EXISTS schema_migrations; "
                             "DROP SCHEMA IF EXISTS phillyfreeschool CASCADE;"
                             "DROP SCHEMA IF EXISTS overseer CASCADE;"
                             "DROP SCHEMA IF EXISTS demo CASCADE;")]))

;;(reset-db)
(defn reset-db []
  (drop-all-tables)
  (migrations/migrate-db @pgdb)
  (init-users))
