(ns overseer.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [compojure.core :as compojure]
            [compojure.coercions :refer [as-int]]
            [clojure.java.io :as io]
            [clojure.tools.nrepl.server :as nrepl-server]
            [refactor-nrepl.middleware :as refactor]
            [jdbc-ring-session.core :refer [jdbc-store]]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [cider.nrepl :as nrepl]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [clojure.tools.trace :as trace]
            [clojure.pprint :as pp]
            [overseer.db :as db]
            [overseer.dates :as dates]
            [overseer.classes :refer [class-routes]]
            [overseer.student :refer [student-routes]]
            [overseer.reports :refer [report-routes]]
            [overseer.database.connection :as conn]
            [overseer.roles :as roles]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [environ.core :refer [env]])
  (:gen-class))

(defroutes app
  (GET "/" []
    (friend/authenticated (io/resource "index.html")))
  (GET "/resetdb" []
    (friend/authorize #{roles/super} ;; (db/reset-db)
                      (resp/redirect "/")))

  (GET "/dates/today" [] (dates/today-string))
  (GET "/sampledb" []
    (friend/authorize #{roles/super} ;; (data/sample-db true)
                      (resp/redirect "/")))

  student-routes
  class-routes
  report-routes

  (GET "/users/login" req
    (io/resource "login.html"))
  (GET "/users/logout" req
    (friend/logout* (resp/redirect (trace/trace "Logout: " "/users/login"))))
  (GET "/users/is-user" req
    (friend/authorize #{roles/user} "You're a user!"))
  (GET "/users/is-admin" req
    (friend/authorize #{roles/admin} (resp/response {:admin true})))
  (route/resources "/")
  (ANY "*" []
    (route/not-found (slurp (io/resource "404.html")))))

(defn my-middleware [app]
  (fn [req]
    (let [schema  (:schema_name (friend/current-authentication req))]
      (binding [db/*school-schema* schema]
        (trace/trace "schema" db/*school-schema*)
        (app req)))))

(defn tapp []
  (-> #'app
      (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn db/get-user)
                            :allow-anon? true
                            :login-uri "/users/login"
                            :default-landing-uri "/"
                            :workflows [(workflows/interactive-form)]})
      (compojure/wrap-routes my-middleware)
      (wrap-session {:store (jdbc-store @conn/pgdb)
                     :cookie-attrs {:max-age (* 3 365 24 3600)}})
      wrap-keyword-params
      wrap-json-body wrap-json-params wrap-json-response ))

(defn -main [& [port]]
  (conn/init-pg)
  (db/init-users)
  (comment (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler))
  (if-let [dev (env :dev)]
    (clojure.java.shell/sh "notify-send" "Server started")
    (nrepl-server/start-server :port 7888 :handler
                               (apply clojure.tools.nrepl.server/default-handler
                                      (concat (map resolve cider.nrepl/cider-middleware)
                                              [refactor/wrap-refactor]))))
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site (tapp)) {:port port :join? false})))
