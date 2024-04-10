(ns overseer.reports
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.coercions :refer [as-int]]
            [ring.util.response :as resp]
            [overseer.db :as db]
            [overseer.queries :as queries]
            [overseer.commands :as cmd]
            [overseer.dates :as dates]
            [overseer.roles :as roles]
            [cemerick.friend :as friend]))

(defn year-resp []
  (let [years (queries/get-years)]
    (resp/response {:years (map :name years)
                    :current_year (dates/get-current-year-string years)})))

(defroutes report-routes
  (GET "/reports/years" []
       (friend/authorize #{roles/user} (year-resp)))

  (DELETE "/reports/years/:year" [year]
          (friend/authorize #{roles/admin}
                            (cmd/delete-year year)
                            (year-resp)))
  (POST "/reports/years" [from_date to_date]
        (friend/authorize #{roles/admin}
                          (let [made? (cmd/make-year from_date to_date)]
                            (resp/response {:made made?}))))
  (GET "/reports/:year" [year]
       (friend/authorize #{roles/admin}
                         (resp/response (queries/get-report year))))
  (GET "/reports/:year/:class" [year class :<< as-int]
       (friend/authorize #{roles/admin}
                         (resp/response (queries/get-report year class))))

  )

