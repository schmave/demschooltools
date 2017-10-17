(ns overseer.student
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.coercions :refer [as-int]]
            [ring.util.response :as resp]
            [clojure.tools.trace :as trace]
            [overseer.db :as db]
            [overseer.queries :as queries]
            [overseer.commands :as cmd]
            [overseer.database.users :as users]
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
  (GET "/allstudents" req
    (friend/authorize #{roles/admin}
                      (resp/response (queries/get-students))))
  (GET "/students" req
       (friend/authorize #{roles/user}
                         (resp/response {:today (dates/today-string)
                                         :students (get-student-list)})))

  (GET "/students/:id" [id :<< as-int]
       (friend/authorize #{roles/user} (student-page-response id)))

  (POST "/students" [name start_date email minutes is_teacher]
        (friend/authorize #{roles/admin}
                          (let [minutes (if (not= nil minutes) (read-string minutes) nil)]
                            (resp/response {:made (cmd/make-student name start_date email is_teacher minutes)
                                            :students (queries/get-students)}))))

  (PUT "/user" [name password]
    (friend/authorize #{roles/super}
                     (users/make-user name password #{roles/admin})))
  (GET "/user" []
    (friend/authorize #{roles/super}
                      (resp/response {:users (users/get-users)})))

  (PUT "/students/:id" [id :<< as-int name start_date email minutes :<< as-int is_teacher]
       (friend/authorize #{roles/admin}
                         (cmd/edit-student id name start_date email is_teacher minutes))
       (student-page-response id))

  (POST "/students/:id/togglehours" [id :<< as-int]
        (friend/authorize #{roles/admin}
                          (do (cmd/edit-student-required-minutes id)
                              (student-page-response id))))

  (POST "/students/:id/absent" [id :<< as-int]
        (friend/authorize #{roles/user}
                          (do (cmd/toggle-student-absent id)
                              (student-page-response id))))

  ;; (POST "/students/:id/maketeacher" [id :<< as-int]
  ;;       (friend/authorize #{roles/user}
  ;;                         (do (cmd/toggle-student-teacher id)
  ;;                             (student-page-response id))))

  (POST "/students/:id/excuse" [id :<< as-int day]
        (friend/authorize #{roles/admin}
                          (cmd/excuse-date id day))
        (student-page-response id))

  (POST "/students/:id/override" [id :<< as-int day]
        (friend/authorize #{roles/admin}
                          (cmd/override-date id day))
        (student-page-response id))

  (POST "/students/:id/swipe/delete" [id :<< as-int swipe]
        (friend/authorize #{roles/admin}
                          (cmd/delete-swipe swipe)
                          (student-page-response id)))
  (POST "/students/:id/swipe" [id :<< as-int direction  missing]
        (friend/authorize #{roles/user}
                          (if (= direction "in")
                            (do (when missing (cmd/swipe-out id missing))
                                (cmd/swipe-in id))
                            (do (when missing (cmd/swipe-in id missing))
                                (cmd/swipe-out id))))
        (resp/response {:students (get-student-list)})))
