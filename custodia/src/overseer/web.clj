(ns overseer.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [compojure.handler :refer [site]]
            [carica.core :as c]
            [compojure.route :as route]
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
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            [environ.core :refer [env]])
  (:gen-class))

(def users {"admin" {:username "admin"
                     :password (creds/hash-bcrypt (env :admin))
                     :roles #{::admin ::user}}
            "super" {:username "super"
                     :password (creds/hash-bcrypt (env :admin))
                     :roles #{::admin ::user ::super}}
            "user" {:username "user"
                    :password (creds/hash-bcrypt (env :userpass))
                    :roles #{::user}}})
(defn year-resp []
  (let [years (data/get-years)]
    (resp/response {:years (map :name years)
                    :current_year (dates/get-current-year-string years)})))

(defn student-page-response [student-id]
  (resp/response {:student (first (att/get-student-with-att student-id))}))

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s )))

(defn should-cast [s]
  (re-find  #"-int" (str s)))

(defmacro RT [type route params & body]
  (let [casts (filter should-cast params)]
    `(~type ~route ~params
            (let [~@(mapcat (fn [cast] `(~cast (parse-int ~cast))) casts)]
              ~@body))))

(comment (pp/pprint (macroexpand-1 '(RT GET "/students/:id-int" [id-int]
                                        (friend/authorize #{::user} (student-page-response id-int))))))


(defroutes app
  (GET "/" [] (friend/authenticated (io/resource "index.html")))
  (GET "/resetdb" []
       (friend/authorize #{::super} ;; (db/reset-db)
                         (resp/redirect "/")))

  (GET "/dates/today" [] (dates/today-string))
  (GET "/sampledb" []
       (friend/authorize #{::super} ;; (data/sample-db true)
                         (resp/redirect "/")))

  (GET "/students" []
    (friend/authorize #{::user}
                      (resp/response (att/get-student-list))))

  (RT GET "/students/:id-int" [id-int]
     (friend/authorize #{::user} (student-page-response id-int)))

  (POST "/students" [name]
    (friend/authorize #{::admin}
                      (let [made? (data/make-student name)]
                        (resp/response {:made made?
                                        :students (att/get-student-list)}))))

  (RT PUT "/students/:id-int" [id-int name]
        (friend/authorize #{::admin}
                          (data/rename id-int name))
        (student-page-response id-int))

  (RT POST "/students/:id-int/togglehours" [id-int]
    (friend/authorize #{::admin}
                      (do (data/toggle-student-older id-int)
                          (student-page-response id-int))))

  (RT POST "/students/:id-int/absent" [id-int]
    (friend/authorize #{::admin}
                      (do (data/toggle-student-absent id-int)
                          (student-page-response id-int))))


  (RT POST "/students/:id-int/excuse" [id-int day]
        (friend/authorize #{::admin}
                          (data/excuse-date id-int day))
        (student-page-response id-int))

  (RT POST "/students/:id-int/override" [id-int day]
        (friend/authorize #{::admin}
                          (data/override-date id-int day))
        (student-page-response id-int))

  (RT POST "/students/:id-int/swipe/delete" [id-int swipe]
        (friend/authorize #{::admin}
                          (data/delete-swipe swipe)
                          (student-page-response id-int)))

  (RT POST "/students/:id-int/swipe" [id-int direction  missing]
        (friend/authorize #{::user}
                          (if (= direction "in")
                            (do (when missing (data/swipe-out id-int missing))
                                (data/swipe-in id-int))
                            (do (when missing (data/swipe-in id-int missing))
                                (data/swipe-out id-int))))
        (resp/response (att/get-student-list)))


  (GET "/reports/:year" [year]
    (friend/authorize #{::admin}
                      (resp/response (db/get-report year))))
  (GET "/year/all" []
       (friend/authorize #{::user} (year-resp)))
  (POST "/year/delete" [year]
        (friend/authorize #{::admin}
                          (data/delete-year year)
                          (year-resp)))
  (POST "/year" [from_date to_date]
        (friend/authorize #{::admin}
                          (let [made? (data/make-year from_date to_date)]
                            (resp/response {:made made?}))))
  (GET "/login" req
       (io/resource "login.html"))
  (GET "/logout" req
       (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/user/is-user" req
       (friend/authorize #{::user} "You're a user!"))
  (GET "/user/is-admin" req
        (friend/authorize #{::admin} (resp/response {:admin true})))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn tapp []
  (-> #'app
      (friend/authenticate {:credential-fn (partial creds/bcrypt-credential-fn users)
                            :allow-anon? true
                            :login-uri "/login"
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

