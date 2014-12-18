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

(defn swipe-form [student-id]
  (html/html [:div
              [:div [:a {:href "/resetdb"} "reset database"]]
              [:div
               [:ul (map (comp (fn [x] [:li (str x)])
                               #(dissoc % :_id :_rev :type))
                         (data/get-swipes student-id))]]
              [:form {:method "POST" :action "/swipe" :role "form"}
               [:button {:class "btn btn-default" :type "submit" :name "direction" :value "out"} "Swipe Out"]
               [:button {:class "btn btn-default" :type "submit" :name "direction" :value "in"} "Swipe In"]]]))

(defn main-form []
  (html/html [:div
              [:div
               [:ul
                (map (fn [s] [:li [:a {:href (str "/swipe/" (:_id s))} (:name s)]])
                     (data/get-students))]]
              [:a {:href "/student/create"} "Create Student"]]))

(enlive/deftemplate main-template "index.html" [form]
  [:p.lead] (enlive/html-content form))

(defn render [p]
  (apply str p))

(defroutes app
  (GET "/" [] (render (main-template (main-form))))
  (GET "/swipe/:sid" [sid] (data/get-swipes sid))
  (GET "/resetdb" [] (data/sample-db) (resp/redirect "/"))
  (POST "/swipe" [direction _id]
        (if (= direction "in")
          (data/swipe-in _id)
          (data/swipe-out _id))
        (resp/redirect (str "/swipe/" _id)))
  (GET "/student/all" [] (data/get-students))
  (GET "/student/create" [] (render (main-template (create-student-form false))))
  (POST "/student/create" req
        (if-let [made? (-> req :params :name data/make-student)]
          (resp/redirect "/")
          (render (main-template (create-student-form true)))))
  (route/resources "/")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn -main [& [port]]
  (nrepl-server/start-server :port 7888 :handler cider-nrepl-handler)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

