(ns overseer.studenttable
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]])
  )

(defn get-today [] "2013-03-02")

(def state (atom {:students []}))

(defn load-students [response]
  ;; (.log js/console (str "got back students!" response))
  (swap! state (fn [st] (assoc st :students response))))

(defn get-students [] (GET "/students" {:handler load-students}) )
(get-students)

(defn signIn [s]
  )
(defn signOut [s]
  )

(defn is-signing-in [s]
  (or (not= nil (:last_swipe_date s))
      (= "out" (:last_swipe_type s))
      (not= (:last_swipe_date s) (get-today))))

(defn get-swipe-button [student way]
  (let [button-icon (if (= way "out") "fa-arrow-left" "fa-arrow-right")
        button-text [:i {:class (str "fa " button-icon)} " "]]
    (if (is-signing-in student)
      [:button.btn.btn-sm.btn-primary {:onClick (signIn student)}
       button-text]
      [:button.btn.btn-sm.btn-info {:onClick (signOut student)}
       button-text])))

(defn make-student-by [pred way students]
  (->> students
       (filter pred)
       (map (fn [student]
              [:div.panel.panel-info.student-listing.col-sm-11
               [:div
                [:Link {:to "student"
                        :params {:studentId (get student "_id")}
                        :id (str "student-" (get student "_id"))}
                 (get student "name")]]
               [:div.attendance-button (get-swipe-button student way)]]))))

(defn get-absents [students]
  (make-student-by (fn [s] (and (not (:in_today s))
                                (:absent_today s)))
                   "absent" students))

(defn get-not-yet-in [students]
  (make-student-by (fn [s] (and (not (:in_today s))
                                (not (:absent_today s))))
                   "notYetIn" students))

(defn get-in-st [students]
  (make-student-by (fn [s] (and (:in_today s)
                                (= "in" (:last_swipe_type s))))
                   "in" students))

(defn get-out [students]
  (make-student-by (fn [s] (and (:in_today s)
                                (= "out" (:last_swipe_type s))))
                   "out" students))

(defn student-table-page []
  (let [students (sort-by #(get % "name") (:students @state))
        absentCol (get-absents students)
        notYetInCol (get-not-yet-in students)
        inCol (get-in-st students)
        outCol (get-out students)]
      [:div.row.student-listing-table
            [:div.col-sm-3.column
                [:div.panel.panel-info.absent
                 [:div.panel-heading.absent
                  [:b (str "Not Coming In (" (count absentCol) ")")]]
                    [:div.panel-body.row absentCol]]]
            [:div.col-sm-3.column.not-in
                [:div.panel.panel-info
                    [:div.panel-heading [:b (str "Not Yet In (" (count notYetInCol) ")")]]
                    [:div.panel-body.row notYetInCol]]]
            [:div.col-sm-3.column.in
                [:div.panel.panel-info
                    [:div.panel-heading [:b "In (" (count inCol) " )"]]
                    [:div.panel-body.row inCol]]]
            [:div.col-sm-3.column.out
                [:div.panel.panel-info
                    [:div.panel-heading [:b "Out (" (count outCol) " )"]]
                    [:div.panel-body.row outCol]]]]))
