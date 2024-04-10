(ns overseer.classes
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.coercions :refer [as-int]]
            [ring.util.response :as resp]
            [overseer.db :as db]
            [overseer.queries :as queries]
            [overseer.commands :as cmd]
            [overseer.dates :as dates]
            [overseer.roles :as roles]
            [cemerick.friend :as friend])
  )

(defroutes class-routes
  (GET "/classes" []
       (friend/authorize #{roles/admin}
                         (resp/response (queries/get-all-classes-and-students))))
  (POST "/classes" [name from_date to_date]
        (friend/authorize #{roles/admin}
                          (cmd/make-class name from_date to_date)
                          (resp/response (queries/get-all-classes-and-students))))

  (POST "/classes/:cid" [cid :<< as-int name from_date to_date minutes :<< as-int late_time]
        (friend/authorize #{roles/admin}
                          (cmd/edit-class cid name from_date to_date minutes late_time)
                          (resp/response (queries/get-all-classes-and-students))))

  (POST "/classes/:cid/student/:sid/add" [cid :<< as-int sid :<< as-int]
        (friend/authorize #{roles/admin}
                          (cmd/add-student-to-class sid cid)
                          (resp/response (queries/get-all-classes-and-students))))

  (POST "/classes/:cid/student/:sid/delete" [cid :<< as-int sid :<< as-int]
        (friend/authorize #{roles/admin}
                          (cmd/delete-student-from-class sid cid)
                          (resp/response (queries/get-all-classes-and-students))))

  (POST "/classes/:cid/activate" [cid :<< as-int]
        (friend/authorize #{roles/admin}
                          (cmd/activate-class cid)
                          (resp/response (queries/get-all-classes-and-students)))))

