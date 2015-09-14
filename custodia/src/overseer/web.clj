(ns overseer.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [compojure.handler :refer [site]]
            [carica.core :as c]
            [compojure.route :as route]
            [compojure.coercions :refer [as-int]]
            [clojure.java.io :as io]
            [clojure.tools.nrepl.server :as nrepl-server]
            [jdbc-ring-session.core :refer [jdbc-store]]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [clojure.tools.trace :as trace]
            [overseer.db :as db]
            [clojure.pprint :as pp]
            [overseer.database :as data]
            [overseer.dates :as dates]
            [overseer.attendance :as att]
            [overseer.classes :refer [class-routes]]
            [overseer.reports :refer [report-routes]]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [overseer.roles :as roles]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [environ.core :refer [env]])
  (:gen-class))

(def users {"admin" {:username "admin"
                     :password (creds/hash-bcrypt (env :admin))
                     :roles #{roles/admin roles/user}}
            "super" {:username "super"
                     :password (creds/hash-bcrypt (env :admin))
                     :roles #{roles/admin roles/user roles/super}}
            "user" {:username "user"
                    :password (creds/hash-bcrypt (env :userpass))
                    :roles #{roles/user}}})

(defn student-page-response [student-id]
  (resp/response {:student (first (att/get-student-with-att student-id))}))

(defn show-archived? []
  (let [{roles :roles} (friend/current-authentication)]
    (contains? roles roles/admin)))

(defn get-student-list []
  (att/get-student-list (show-archived?)))

(defroutes app
  (GET "/" [] (friend/authenticated (io/resource "index.html")))
  (GET "/resetdb" []
       (friend/authorize #{roles/super} ;; (db/reset-db)
                         (resp/redirect "/")))

  (GET "/dates/today" [] (dates/today-string))
  (GET "/sampledb" []
       (friend/authorize #{roles/super} ;; (data/sample-db true)
                         (resp/redirect "/")))

  (GET "/students" req
       (friend/authorize #{roles/user}
                         (resp/response (get-student-list))))

  (GET "/students/:id" [id :<< as-int]
     (friend/authorize #{roles/user} (student-page-response id)))

  (POST "/students" [name]
        (friend/authorize #{roles/admin}
                          (let [made? (data/make-student name)]
                            (resp/response {:made made?
                                            :students (get-student-list)}))))

  (PUT "/students/:id" [id :<< as-int name]
        (friend/authorize #{roles/admin}
                          (data/rename id name))
        (student-page-response id))

  (POST "/students/:id/togglearchived" [id :<< as-int]
        (friend/authorize #{roles/admin}
                          (do (data/toggle-student-archived id)
                              (student-page-response id))))

  (POST "/students/:id/togglehours" [id :<< as-int]
    (friend/authorize #{roles/admin}
                      (do (data/toggle-student-older id)
                          (student-page-response id))))

  (POST "/students/:id/absent" [id :<< as-int]
    (friend/authorize #{roles/user}
                      (do (data/toggle-student-absent id)
                          (student-page-response id))))

  (POST "/students/:id/excuse" [id :<< as-int day]
        (friend/authorize #{roles/admin}
                          (data/excuse-date id day))
        (student-page-response id))

  (POST "/students/:id/override" [id :<< as-int day]
        (friend/authorize #{roles/admin}
                          (data/override-date id day))
        (student-page-response id))

  (POST "/students/:id/swipe/delete" [id :<< as-int swipe]
        (friend/authorize #{roles/admin}
                          (data/delete-swipe swipe)
                          (student-page-response id)))

  (POST "/students/:id/swipe" [id :<< as-int direction  missing]
        (friend/authorize #{roles/user}
                          (if (= direction "in")
                            (do (when missing (data/swipe-out id missing))
                                (data/swipe-in id))
                            (do (when missing (data/swipe-in id missing))
                                (data/swipe-out id))))
        (resp/response (get-student-list)))

  class-routes
  report-routes

  (GET "/users/login" req
       (io/resource "login.html"))
  (GET "/users/logout" req
       (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/users/is-user" req
       (friend/authorize #{roles/user} "You're a user!"))
  (GET "/users/is-admin" req
        (friend/authorize #{roles/admin} (resp/response {:admin true})))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn tapp []
  (-> #'app
      (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn users)
                            :allow-anon? true
                            :login-uri "/users/login"
                            :default-landing-uri "/"
                            :workflows [(workflows/interactive-form)]})
      (wrap-session {:store (jdbc-store @db/pgdb)
                     :cookie-attrs {:max-age (* 3 365 24 3600)}})
      wrap-keyword-params
      wrap-json-body wrap-json-params wrap-json-response ))

(defn -main [& [port]]
  (db/init-pg)
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site (tapp)) {:port port :join? false})))
