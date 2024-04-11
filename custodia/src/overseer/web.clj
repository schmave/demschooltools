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
            [overseer.commands :as cmd]
            [overseer.dst-imports :as dst]
            [overseer.queries :as queries]
            [overseer.database.sample-db :refer [sample-db]]
            [overseer.dates :as dates]
            [overseer.classes :refer [class-routes]]
            [overseer.student :refer [student-routes]]
            [overseer.reports :refer [report-routes]]
            [overseer.database.connection :as conn]

            [overseer.migrations :as migrations]
            [overseer.database.users :as users]
            [overseer.roles :as roles]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [stencil.core :refer [render-string]]
            [environ.core :refer [env]]

            [overseer.db :as d])
  (:gen-class))

(defn read-template [filename]
  (slurp (clojure.java.io/resource filename)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def start-id (subs (uuid) 0 7))

(defroutes app
  (POST "/updatefromdst" req
        (friend/authorize #{roles/admin}
                          (resp/response (dst/update-from-dst))))

  (GET "/" []
       (friend/authenticated
        (render-string (read-template "index.html") {:id start-id})))

  (PUT "/school/:school_id" [school_id :<< as-int]
       (friend/authorize #{roles/super}
                         (users/set-user-school (:user_id (users/get-user "super")) school_id)))

  (GET "/schools" []
       (friend/authorize #{roles/super}
                         (resp/response
                          (let [schools (queries/get-schools)
                                superSchool (queries/get-current-school)]
                            {:schools schools
                             :school superSchool}))))

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
  (POST "/users/password" [username password]
        (friend/authorize #{roles/admin}
                          (users/change-password username password)))
  (GET "/users/login" req
       (io/resource "login.html"))
  (GET "/users/logout" req
       (friend/logout* (resp/redirect (log/spyf "Logout: %s" "/users/login"))))
  (GET "/users/is-user" req
       (friend/authorize #{roles/user} {:message "You're a user!"
                                        :school (queries/get-current-school)}))
  (GET "/users/is-admin" req
       (resp/response
        {:admin (-> req friend/current-authentication :roles roles/admin)
         :school (queries/get-current-school)}))
  (GET "/users/is-super" req
       (let [user (users/get-user "super")]
         (resp/response {:super (-> req friend/current-authentication :roles roles/super)
                         :school (queries/get-current-school)})))

  (GET "/js/gen/:id{.+}/app.js" req
       (io/resource "public/js/gen/app.js"))
  (GET "/css/:id{.+}/starter-template.css" req
       (io/resource "public/starter-template.css"))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn bind-session-school [school-id app req]
  (binding [db/*school-id* school-id
            db/*school-timezone* (:timezone (queries/get-schools school-id))]
    (app req)))

(defn my-middleware [app]
  (fn [req]
    (let [auth (friend/current-authentication req)
          school-id (:school_id auth)
          username (:username auth)]
      (if (= "super" username)
        (bind-session-school (:school_id (users/get-user username)) app req)
        (bind-session-school school-id app req)))))

(defn wrap-exception-handling
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Exception")
        {:status 500 :body "Server Error"}))))

(def production?
  (= "production" (env :appenv)))

(def development?
  (not production?))

(defn wrap-if [handler pred wrapper & args]
  (if pred
    (apply wrapper handler args)
    handler))

(defn tapp []
  (-> #'app
      (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn users/get-user)
                            :allow-anon? true
                            :login-uri "/users/login"
                            :default-landing-uri "/"
                            :workflows [(workflows/interactive-form)]})
      (compojure/wrap-routes my-middleware)
      (wrap-if development? wrap-reload)
      (wrap-session {:store (jdbc-store @conn/pgdb {:table :overseer.session_store})
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
  (when (= "true" (env :migratedb))
    (migrations/migrate-db @conn/pgdb))
  (when (= "true" (env :newdb))
    (do (users/reset-db)
        (sample-db)))
  (users/init-users)
  (if (= "true" (env :notify))
    (do (print "Server started")
        (sh/sh "notify-send" "-u" "critical" "Server started")))
  (let [port (Integer. (or port (env :port) "5000"))
        host (or "127.0.0.1" (env :host))]
    (jetty/run-jetty (site (tapp)) {:port port :host host :join? false})))

(defn -main [& [port]]
  (start-site port))
