(ns clojure-getting-started.database
  (:require [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
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

(defn get-students
  ([]  (get-students nil))
  ([ids] (get-* "students" ids)))

(defn make-student [name]
  (when (empty? (get-students name))
    (couch/put-document db/db {:type :student :name name})))

(defn swipe-in [id]
  (couch/put-document db/db {:type :swipe :student_id id :in_time (str (t/now))}))

(defn get-swipes [id]
  (couch/get-view db/db "view" "swipes" {:keys [id]}))

(defn- lookup-in-swipe [id]
  (-> (get-swipes id)
      last
      :value))
(defn swiped-in? [in-swipe] (and in-swipe (not (:out_time in-swipe))))
(defn- ask-for-in-swipe [id] (swipe-in id))

(defn swipe-out [id]
  (let [in-swipe (lookup-in-swipe id)]
    (if (swiped-in? in-swipe)
      (couch/put-document db/db (assoc in-swipe :out_time (str (t/now))))
      (let [in-swipe (ask-for-in-swipe id)]
        (couch/put-document db/db (assoc in-swipe :out_time (str (t/now))))))))

;; (sample-db)   
(defn sample-db []
  (couch/delete-database db/db)
  (couch/create-database db/db)
  (make-db)
  (let [s (:_id (make-student "steve"))]
    (swipe-in s))
  ;; (swipe-out 1)
  ;; (get-students)
  ;; (get-students)
  ;; (get-students "steve")
  )
