(ns clojure-getting-started.database
  (:require [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clojure-getting-started.helpers :refer :all]
            [clojure.tools.trace :as trace]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            ))

(def design-doc
  {"_id" "_design/view"
   "views" {"students" {"map" "function(doc) {
                                 if (doc.type === \"student\") {
                                   emit(doc._id, doc);
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

(defn swipe-in
  ([id] (swipe-in id (t/now)))
  ([id time] 
     (let [last-swipe (lookup-last-swipe id)]
       (if (only-swiped-in? last-swipe)
         (+ 1 1);; (ask-for-out-swipe)
         (couch/put-document db/db
                             {:type :swipe :student_id id :in_time (str time)})))))

(defn- ask-for-in-swipe [id] (swipe-in id))

(defn swipe-out
  ([id] (swipe-out id (t/now)))
  ([id time] (let [last-swipe (lookup-last-swipe id)]
               (if (only-swiped-in? last-swipe)
                 (couch/put-document db/db (assoc last-swipe :out_time (str time)))
                 #_(let [in-swipe (ask-for-in-swipe id)]
                     (couch/put-document db/db (assoc in-swipe :out_time (str time))))))))

(defn get-hours-needed [id]
  ;; TODO implement this
  5)

(def date-format (f/formatter "MM-dd-yyyy"))
(def time-format (f/formatter "hh:mm:ss"))
;; TODO make this configurable?
(def local-time-zone-id (t/time-zone-for-id "America/New_York"))

(defn format-to-local [d f]
  (f/unparse (f/with-zone f local-time-zone-id)
             (f/parse d)))

(defn make-date-string [d]
  (when d (format-to-local d date-format)))
(defn make-time-string [d]
  (when d (format-to-local d time-format)))

(defn clean-dates [swipe]
  (?assoc swipe
          :nice_in_time (make-time-string (:in_time swipe))
          :nice_out_time (make-time-string (:out_time swipe))))

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
  (make-date-string (:in_time swipe)))

(defn append-interval [swipe]
  (if (:out_time swipe)
    (let [int (t/interval (f/parse (:in_time swipe))
                          (f/parse (:out_time swipe)))
          int-hours (t/in-minutes int)]
      (assoc swipe :interval int-hours))
    swipe))

(defn get-attendance [id]
  (tracelet [min-hours (get-hours-needed id)
             swipes (get-swipes id)
             swipes (map append-interval swipes)
             swipes (map clean-dates swipes)
             grouped-swipes (group-by swipe-day swipes)
             summed-days (map #(append-validity min-hours %) grouped-swipes)]
            {:total_days (count (filter :valid summed-days))
             :total_abs (count (filter (comp not :valid) summed-days))
             :days summed-days}))

(defn get-students
  ([] (get-students nil))
  ([ids]
     (map #(merge (get-attendance (:_id %)) %)
          (get-* "students" ids))))

(defn student-not-yet-created [name]
  (empty? (filter #(= name (:name %)) (get-students))))

(defn make-student [name]
  (when (student-not-yet-created name)
    (couch/put-document db/db {:type :student :name name})))

;; (sample-db)   
(defn sample-db []
  (couch/delete-database db/db)
  (couch/create-database db/db)
  (make-db)
  (make-student "steve")
  (make-student "jim")
  ;; (swipe-out 1)
  ;; (get-students)
  ;; (get-students)
  ;; (get-students "steve")
  )
