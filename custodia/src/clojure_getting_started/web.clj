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
            [clojure-getting-started.database :as data]
            [clj-time.core :as t]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as html]
            [environ.core :refer [env]]))
(defn t []
  (html/html [:form {:method "POST" :action "/student/create"}
              [:label {:name "name"}]
              [:input {:type "text" :name "name"}]
              [:input {:type "submit" :value "Create"}]
              ]))

(enlive/deftemplate main-template "index.html" []
  [:p.lead] (enlive/content (t)))

(defroutes app
  (GET "/" [] (apply str (main-template)))
  (POST "/student/create" req "success")
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

