(ns overseer.studenttable
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]])
  )

(def state (atom nil))

(defn get-student [student way]
  (let [button ]
    [:div.panel.panel-info.student-listing.col-sm-11
     [:div
      [:Link {:to "student"
              :params {:studentId student._id}
              :id (str "student-" + student._id)}
       student.name]]
     [:div.attendance-button (getSwipeButton student way)]]))

(defn student-table-page []
  (let [absentCol []
        notYetInCol []
        inCol []
        outCol []]
      [:div.row.student-listing-table
            [:div.col-sm-3.column
                [:div.panel.panel-info.absent
                 [:div.panel-heading.absent
                  [:b (str "Not Coming In (" (length absentCol) ")")]]
                    [:div.panel-body.row absentCol]]]
            [:div.col-sm-3.column.not-in
                [:div.panel.panel-info
                    [:div.panel-heading [:b (str "Not Yet In (" (length notYetInCol) ")")]]
                    [:div.panel-body.row notYetInCol]]]
            [:div.col-sm-3.column.in
                [:div.panel.panel-info
                    [:div.panel-heading [:b "In (" (length inCol) " )"]]
                    [:div.panel-body.row inCol]]]
            [:div.col-sm-3.column.out
                [:div.panel.panel-info
                    [:div.panel-heading [:b "Out (" (length outCol) " )"]]
                    [:div.panel-body.row outCol]]]]))
