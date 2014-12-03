(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [clojure.tools.nrepl.server :as nrepl-server]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [ring.adapter.jetty :as jetty]
            [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clj-time.core :as t]
            [environ.core :refer [env]]))

;; (def i  (couch/put-document db/db {:test "test"}))
;; (couch/get-document db/db (:_id i))

(defn make-student [name]
  (couch/put-document db/db {:type :student }))

(defn swipe-in [id]
  (couch/put-document db/db {:type :swipe-in :id id :time (t/now)}))

(defn swipe-in [id]
  (couch/put-document db/db {:type :swipe-out :id id :time (t/now)}))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Test"})

(defroutes app
  (GET "/" [] (splash))
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

