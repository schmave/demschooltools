(ns clojure-getting-started.database
  (:require [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clj-time.core :as t]))

(def design-doc
  {"_id" "_design/view"
   "views" {"students" {"map" "function(doc) {
                                 if (doc.type === \"student\") {
                                   emit(doc.id, doc);
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
                           emit(doc.id, doc);
                         }
                       }"}
            }
   "language" "javascript"})


(defn make-db [] (couch/put-document db/db design-doc))
(defn make-student [name]
  (couch/put-document db/db {:type :student :id 1 :name name}))

(defn get-students
  ([] (get-students nil))
  ([ids]
     (map :value
          (if ids
            (couch/get-view db/db "view" "students" {:keys (if (coll? ids) ids [ids])})
            (couch/get-view db/db "view" "students")))))

(defn swipe-in [id]
  (couch/put-document db/db {:type :swipe :id id :in-time (str (t/now))}))

(defn get-swipes [id]
  (couch/get-view db/db "view" "swipes" {:keys [id]}))
(defn- lookup-in-swipe [id]
  (-> (get-swipes id)
      last
      :value))
(defn swiped-in? [in-swipe] (and in-swipe (not (:out-time in-swipe))))
(defn- ask-for-in-swipe [id] (swipe-in id))

(defn swipe-out [id]
  (let [in-swipe (lookup-in-swipe id)]
    (if (swiped-in? in-swipe)
      (couch/put-document db/db (assoc in-swipe :out-time (str (t/now))))
      (let [in-swipe (ask-for-in-swipe id)]
        (couch/put-document db/db (assoc in-swipe :out-time (str (t/now))))))))

;; (sample-db)
(defn sample-db []
  (couch/delete-database db/db)
  (couch/create-database db/db)
  (make-db)
  ;; (make-student "steve")
  ;; (swipe-in 1)
  ;; (swipe-out 1)
  ;; (get-students)
  ;; (get-students)
  )
