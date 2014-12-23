(ns clojure-getting-started.database
  (:require [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clojure.tools.trace :as trace]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]))

(def design-doc
  {"_id" "_design/view"
   "views" {"students" {"map" "function(doc) {
                                 if (doc.type === \"student\") {
                                   emit(doc.name, doc);
                                 }
                               }"}
            "student-swipes" {"map"
                              "function(doc) {
                                if (doc.type == \"student\") {
                                  map([doc._id, 0], doc);
                                } else if (doc.type == \"swipe\") {
                                  map([doc.post, 1], doc);
                                }
                              }"}
            "swipes" {"map"
                      "function(doc) {
                         if (doc.type == \"swipe\") {
                           emit(doc.student_id, doc);
                         }
                       }"}
            }
   "language" "javascript"})

(defn make-db [] (couch/put-document db/db design-doc))

(defn get-* [type ids]
  (map :value
       (if ids
         (couch/get-view db/db "view" type {:keys (if (coll? ids) ids [ids])})
         (couch/get-view db/db "view" type))))


(defn get-swipes [ids]
  (get-* "swipes" ids))

(defn- lookup-last-swipe [id]
  (-> (get-swipes id)
      last))

(defn only-swiped-in? [in-swipe] (and in-swipe (not (:out_time in-swipe))))

(defn swipe-in [id]
  (let [last-swipe (lookup-last-swipe id)]
    (if (only-swiped-in? last-swipe)
      (+ 1 1);; (ask-for-out-swipe)
      (couch/put-document db/db
                          {:type :swipe :student_id id :in_time (str (l/local-now))}))))

(defn- ask-for-in-swipe [id] (swipe-in id))

(defn swipe-out [id]
  (let [last-swipe (lookup-last-swipe id)]
    (if (only-swiped-in? last-swipe)
      (couch/put-document db/db (assoc last-swipe :out_time (str (l/local-now))))
      #_(let [in-swipe (ask-for-in-swipe id)]
          (couch/put-document db/db (assoc in-swipe :out_time (str (l/local-now))))))))

(defn get-hours-needed [id]
  ;; TODO implement this
  5)

(def date-format (f/formatter "MM-dd-yyyy"))
(def time-format (f/formatter "hh:mm:ss"))

(defn clean-dates [swipe]
  (let [in-ed (assoc swipe
                :nice_in_time
                (f/unparse time-format (f/parse (:in_time swipe))))]
    (if (:out_time swipe)
      (assoc in-ed :nice_out_time (f/unparse time-format (f/parse (:out_time swipe))))
      in-ed)))

;; (get-attendance  "fa5a8a9cbef3dbb6b0bc2733ed00a7db")  
(defn append-validity [min-hours swipes]
  (let [int-mins (->> swipes
                      second
                      (map (comp :interval))
                      (filter (comp not nil?))
                      (reduce +))
        int-hours (/ int-mins 60)]
    {:valid (> int-hours min-hours)
     :day (first swipes)
     :total_mins int-mins
     :swipes (second swipes)}))

(defn swipe-day [swipe]
  (f/unparse date-format (f/parse (:in_time swipe))))

(defn append-interval [swipe]
  (if (:out_time swipe)
    (let [int (t/interval (f/parse (:in_time swipe)) (f/parse (:out_time swipe)))
          int-hours (t/in-minutes int)]
      (assoc swipe :interval int-hours))))

(defn get-attendance [id]
  (let [min-hours (get-hours-needed id)
        swipes (get-swipes id)
        swipes (map append-interval swipes)
        swipes (map clean-dates swipes)
        grouped-swipes (group-by swipe-day swipes)
        summed-days (map #(append-validity min-hours %) grouped-swipes)]
    {:total_days (count (filter :valid summed-days))
     :total_abs (count (filter (comp not :valid) summed-days))
     :days summed-days}))
(get-attendance  "fa5a8a9cbef3dbb6b0bc2733ed00a7db")

(defn get-students
  ([]  (get-students nil))
  ([ids]
     (map #(merge (get-attendance (:_id %)) %)
          (get-* "students" ids))))

(defn make-student [name]
  (when (empty? (get-students name))
    (couch/put-document db/db {:type :student :name name})))

;; (sample-db)   
(defn sample-db []
  (couch/delete-database db/db)
  (couch/create-database db/db)
  (make-db)
  (make-student "steve")
  ;; (swipe-out 1)
  ;; (get-students)
  ;; (get-students)
  ;; (get-students "steve")
  )
