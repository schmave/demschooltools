(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [clojure.tools.nrepl.server :as nrepl-server]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clojure-getting-started.database :as data]
            [clj-time.core :as t]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as html]
            [environ.core :refer [env]]))

(def create-student-form 
  (html/html [:form {:method "POST" :action "/student/create" :role "form"}
              [:div {:class "form-group"}
               [:label {:for "s-name" :name "name"} "Name"]
               [:input {:class "form-control" :id "s-name" :type "text" :name "name"}]]
              [:button {:class "btn btn-default" :type "submit" :value "Create"}]]))

(defn main-form []
  (html/html [:div
              [:a {:href "/student/create"} "Create Student"]]))

(enlive/deftemplate main-template "index.html" [form]
  [:p.lead] (enlive/html-content form ))

(defroutes app
  (GET "/" [] (apply str (main-template (main-form))))
  (GET "/student/create" [] (apply str (main-template create-student-form)))
  (POST "/student/create" req (resp/redirect "/"))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

