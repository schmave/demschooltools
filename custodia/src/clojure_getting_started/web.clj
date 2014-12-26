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
            [environ.core :refer [env]]))

(defroutes app
  (GET "/" [] (io/resource "index.html"))
  (GET "/swipe/:sid" [sid]  (resp/response (data/get-attendance sid)))
  (GET "/resetdb" [] (data/sample-db) (resp/redirect "/"))
  (POST "/swipe" [direction _id]
        (if (= direction "in")
          (data/swipe-in _id)
          (data/swipe-out _id))
        (resp/response (first (data/get-students [_id]))))
  (GET "/student/all" [] (data/get-students))
  (POST "/student/create" [name]
        (let [made? (data/make-student name)]
          {:made made? :students (data/get-students)}))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(def tapp (-> #'app wrap-json-body wrap-json-params wrap-json-response))

(defn -main [& [port]]
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'tapp) {:port port :join? false})))

