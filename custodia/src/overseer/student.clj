(ns overseer.student
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.coercions :refer [as-int]]
            [ring.util.response :as resp]
            [clojure.tools.trace :as trace]
            [overseer.db :as db]
            [overseer.database :as data]
            [overseer.dates :as dates]
            [overseer.attendance :as att]
            [overseer.roles :as roles]
            [cemerick.friend :as friend]))

(defn student-page-response [student-id]
  (resp/response {:student (first (att/get-student-with-att student-id))}))

(defn show-archived? [] true)

(defn get-student-list []
  (att/get-student-list (show-archived?)))

(defroutes student-routes
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
        (trace/trace "Posted" [id direction missing])
        (friend/authorize #{roles/user}
                          (if (= direction "in")
                            (do (when missing (data/swipe-out id missing))
                                (data/swipe-in id))
                            (do (when missing (data/swipe-in id missing))
                                (data/swipe-out id))))
        (resp/response (get-student-list))))
