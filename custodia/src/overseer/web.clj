(ns overseer.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [clojure.java.shell :as sh]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [compojure.core :as compojure]
            [compojure.coercions :refer [as-int]]
            [clojure.java.io :as io]
            [jdbc-ring-session.core :refer [jdbc-store]]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [overseer.db :as db]
            [overseer.database.sample-db :as sampledb]
            [overseer.dates :as dates]
            [overseer.classes :refer [class-routes]]
            [overseer.student :refer [student-routes]]
            [overseer.reports :refer [report-routes]]
            [overseer.database.connection :as conn]
            [overseer.database.users :as users]
            [overseer.roles :as roles]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [stencil.core :refer [render-string]]
            [environ.core :refer [env]]
            )
  (:gen-class))

(defn read-template [filename]
  (slurp (clojure.java.io/resource filename)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def start-id (subs (uuid) 0 7))

(defroutes app
  (GET "/" []
    (friend/authenticated
     (render-string (read-template "index.html") {:id start-id})))

  (PUT "/school/:school_id" [school_id :<< as-int]
    (friend/authorize #{roles/super}
                      (users/set-user-school (:user_id (users/get-user "super")) school_id)))

  (GET "/schools" []
    (friend/authorize #{roles/super}
                      (resp/response
                       (let [schools (db/get-schools)
                             superSchoolId (:school_id (users/get-user "super"))
                             superSchool (first (filter (fn [s] (= superSchoolId (:_id s))) schools))]
                         {:schools schools
                          :superSelectedSchool superSchool}))))

  (GET "/hello" []
    (friend/authorize #{roles/super}
                      (resp/response "there")))

  student-routes
  class-routes
  report-routes

  (POST "/email" [email]
    (users/insert-email email)
    (resp/redirect "/users/login"))

  (GET "/about" req
    (io/resource "about.html"))
  (GET "/users/login" req
    (io/resource "login.html"))
  (GET "/users/logout" req
    (friend/logout* (resp/redirect (log/spyf "Logout: %s" "/users/login"))))
  (GET "/users/is-user" req
    (friend/authorize #{roles/user} "You're a user!"))
  (GET "/users/is-admin" req
    (resp/response {:admin (-> req friend/current-authentication :roles roles/admin)}))
  (GET "/users/is-super" req
    (let [user (users/get-user "super")]
      (resp/response {:super (-> req friend/current-authentication :roles roles/super)
                      :schema (:schema_name user)})))

  (GET "/js/gen/:id{.+}/app.js" req
    (io/resource "public/js/gen/app.js"))
  (route/resources "/")
  (ANY "*" []
    (route/not-found (slurp (io/resource "404.html")))))

(defn my-middleware [app]
  (fn [req]
    (let [auth (friend/current-authentication req)
          schema (:school_id auth)
          username (:username auth)]
      (if (= "super" username)
        (let [schema (:school_id (users/get-user username))]
          (binding [db/*school-id* schema]
            (app req)))
        (binding [db/*school-id* schema]
          (app req))))))

(defn wrap-exception-handling
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Exception")
        {:status 400 :body "Invalid data"}))))

(defn tapp []
  (-> #'app
      (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn users/get-user)
                            :allow-anon? true
                            :login-uri "/users/login"
                            :default-landing-uri "/"
                            :workflows [(workflows/interactive-form)]})
      (compojure/wrap-routes my-middleware)
      wrap-reload
      (wrap-session {:store (jdbc-store @conn/pgdb)
                     :cookie-attrs {:max-age (* 3 365 24 3600)}})
      wrap-not-modified
      wrap-keyword-params
      wrap-json-body
      wrap-json-params
      wrap-json-response
      wrap-exception-handling
      ))

;;(start-site 5000)  
(defn start-site [port]
  (conn/init-pg)
  (if (env :migratedb)
    (do (users/reset-db)
        (sampledb/sample-db)))
  (users/init-users)
  (if (env :notify)
    (do (print "Server started")
        (sh/sh "notify-send" "-u" "critical" "Server started")))
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site (tapp)) {:port port :join? false})))

(defn -main [& [port]]
  (start-site port))
