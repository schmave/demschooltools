(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [clojure.tools.nrepl.server :as nrepl-server]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [clojure.tools.trace :as trace]
            [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clojure-getting-started.database :as data]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as html]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [environ.core :refer [env]]))

(def users {"admin" {:username "admin"
                     :password (creds/hash-bcrypt "test")
                     :roles #{::admin ::user}}
            "user" {:username "user"
                    :password (creds/hash-bcrypt "test")
                    :roles #{::user}}})

(defroutes app
  (GET "/" [] (friend/authenticated (io/resource "index.html")))
  (GET "/swipe/:sid" [sid year]
       (friend/authorize #{::user}
                         (resp/response (data/get-attendance year sid))))
  (GET "/resetdb" [] (data/sample-db) (resp/redirect "/"))
  (POST "/override" [_id day year]
        (friend/authorize #{::admin}
                          (data/override-date _id day))
        (resp/response (first (data/get-students-with-att year [_id]))))
  (POST "/swipe" [direction _id year]
        (friend/authorize #{::user}
                          (if (= direction "in")
                            (data/swipe-in _id)
                            (data/swipe-out _id)))
        (resp/response (first (data/get-students-with-att year [_id]))))
  (GET "/year/all" [year]
       (friend/authorize #{::user}
                         (map :name (data/get-years))))
  (POST "/student/all" [year]
        (friend/authorize #{::user}
                          (trace/trace "year" year)
                          (trace/trace "get-studnets" (data/get-students-with-att year))))
  (POST "/student/create" [name year]
        (friend/authorize #{::admin}
                          (let [made? (data/make-student name)]
                            (resp/response {:made made? :students (data/get-students-with-att year)}))))
  (POST "/year/create" [from_date to_date]
        (friend/authorize #{::admin}
                          (let [made? (data/make-year from_date to_date)]
                            (resp/response {:made made?}))))
  (GET "/login" req
       (io/resource "login.html"))
  (GET "/logout" req
       (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/is-user" req
       (friend/authorize #{::user} "You're a user!"))
  (POST "/is-admin" req
        (friend/authorize #{::admin} (resp/response {:admin true})))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(def tapp (-> #'app
              (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn users)
                                    :allow-anon? true
                                    :login-uri "/login"
                                    :default-landing-uri "/"
                                    :workflows [(workflows/interactive-form)]})
              wrap-json-body wrap-json-params wrap-json-response))

(defn -main [& [port]]
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'tapp) {:port port :join? false})))

