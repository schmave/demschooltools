(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
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
            [clj-time.core :as t]
            [net.cgrand.enlive-html :as enlive]
            [hiccup.core :as html]
            [environ.core :refer [env]]))

(defn create-student-form [show-student-exists?] 
  (html/html [:div [:form {:method "POST" :action "/student/create" :role "form"}
                    [:div {:class "form-group"}
                     [:label {:for "s-name" :name "name"} "Name"]
                     [:input {:class "form-control" :id "s-name" :type "text" :name "name"}]]
                    [:button {:class "btn btn-default" :type "submit" :value "Create"} "Create"]]
              (when show-student-exists?
                [:div "A student with that name already exists"])]))

(def swipe-form 
  (html/html [:form {:method "POST" :action "/swipe/in" :role "form"}
              [:div {:class "form-group"}
               [:select {:name "_id"}
                (map (fn [s] [:option {:value (:_id s)} (:name s)])
                     (data/get-students))]]
              [:button {:class "btn btn-default" :type "submit" :value "Swipe In"} "Swipe In"]]))

(defn main-form []
  (html/html [:div
              [:a {:href "/swipe"} "Swipe page"]
              [:div
               [:ul
                (map (fn [s] [:li (:name s)]) (data/get-students))]]
              [:a {:href "/student/create"} "Create Student"]]))

(enlive/deftemplate main-template "index.html" [form]
  [:p.lead] (enlive/html-content form))

(defroutes app
  (GET "/" [] (apply str (main-template (main-form))))
  (GET "/swipe" [] (apply str (main-template swipe-form)))
  (POST "/swipe/in" req
        (data/swipe-in (-> req :params :_id))
        (apply str (main-template swipe-form)))
  (GET "/student/create" [] (apply str (main-template (create-student-form false))))
  (POST "/student/create" req
        (if-let [made? (-> req :params :name data/make-student)]
          (resp/redirect "/")
          (apply str (main-template (create-student-form true)))))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

