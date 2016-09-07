(ns overseer.classes
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.coercions :refer [as-int]]
            [ring.util.response :as resp]
            [clojure.tools.trace :as trace]
            [overseer.db :as db]
            [overseer.database :as data]
            [overseer.dates :as dates]
            [overseer.roles :as roles]
            [cemerick.friend :as friend])
  )

(defroutes class-routes
  (GET "/classes" []
       (friend/authorize #{roles/admin}
                         (resp/response (db/get-all-classes-and-students))))
  (POST "/classes" [name from_date to_date]
        (friend/authorize #{roles/admin}
                          (data/make-class name from_date to_date)
                          (resp/response (db/get-all-classes-and-students))))

  (POST "/classes/:cid/student/:sid/add" [cid :<< as-int sid :<< as-int]
        (friend/authorize #{roles/admin}
                          (data/add-student-to-class sid cid)
                          (resp/response (db/get-all-classes-and-students))))

  (POST "/classes/:cid/student/:sid/delete" [cid :<< as-int sid :<< as-int]
        (friend/authorize #{roles/admin}
                          (db/delete-student-from-class sid cid)
                          (resp/response (db/get-all-classes-and-students))))

  (POST "/classes/:cid/activate" [cid :<< as-int]
        (friend/authorize #{roles/admin}
                          (db/activate-class cid)
                          (resp/response (db/get-all-classes-and-students)))))

