(ns clojure-getting-started.database
  (:require [com.ashafa.clutch :as couch]
            [clojure-getting-started.db :as db]
            [clojure-getting-started.helpers :refer :all]
            [clojure-getting-started.dates :refer :all]
            [clojure.tools.trace :as trace]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            ))

(defn get-* [type ids]
  (let [results (map :value
                     (if ids
                       (couch/get-view db/db "view" type {:keys (if (coll? ids) ids [ids])})
                       (couch/get-view db/db "view" type )))
        results (sort-by :inserted-date results)]
    results))

(defn get-swipes
  ([] (get-swipes nil))
  ([ids]
     (get-* "swipes" ids)))

(defn get-overrides [ids]
  (get-* "overrides" ids))

(defn- lookup-last-swipe [id]
  (-> (get-swipes id)
      last))

(defn only-swiped-in? [in-swipe] (and in-swipe (not (:out_time in-swipe))))

(declare swipe-out)
(defn make-swipe [student-id]
  {:type :swipe :student_id student-id :in_time nil :out_time nil})

(defn persist! [doc]
  (couch/put-document db/db (assoc doc :inserted-date
                                   (or (:inserted-date doc)
                                       (str (t/now))))))

(trace/deftrace swipe-in
  ([id] (swipe-in id (t/now)))
  ([id in-time & [missing-out]] 
     (let [last-swipe (lookup-last-swipe id)]
       (when (only-swiped-in? last-swipe)
         (swipe-out id missing-out))
       (persist! (assoc (make-swipe id) :in_time (str in-time))))))


(defn sanitize-out [swipe]
  (let [in (:in_time swipe)
        in (when in (f/parse in))
        out (:out_time swipe)
        out (when out (f/parse out))]
    (if (and in out)
      (if (or (not (t/before? in out))
              (not (= (t/day in) (t/day out))))
        (assoc swipe :out_time (:in_time swipe))
        swipe)
      swipe)))

;; (sample-db)   
(trace/deftrace swipe-out
  ([id] (swipe-out id (t/now)))
  ([id out-time & [missing-in]]
     (let [last-swipe (lookup-last-swipe id)
           in-swipe (if (only-swiped-in? last-swipe) last-swipe 
                        (assoc (make-swipe id) :in_time (str missing-in)))
           out-swipe (assoc in-swipe :out_time (str out-time))
           out-swipe (sanitize-out out-swipe)]
       (persist! out-swipe))))

;; TODO - make multimethod on type
(defn get-years
  ([] (get-years nil))
  ([names]
     (get-* "years" names)))

(defn delete-year [year]
  (when-let [year (first (get-years year))]
    (couch/delete-document db/db year)))


(defn override-date [id date-string]
  (->> {:type :override
        :student_id id
        :date date-string}
       persist!))

(defn get-students
  ([] (get-students nil))
  ([ids] (get-* "students" ids)))

;; (get-years)    
(defn student-not-yet-created [name]
  (empty? (filter #(= name (:name %)) (get-students))))

(defn make-student [name]
  (when (student-not-yet-created name)
    (persist! {:type :student :name name :olderdate nil})))

(defn- toggle-older [older]
  (if older nil (str (t/now))))

(defn toggle-student [_id]
  (let [student (first (get-students _id))] 
    (persist! (assoc student :olderdate (toggle-older (:olderdate student))))))

(defn make-year [from to]
  (let [from (f/parse from)
        to (f/parse to)
        name (str (f/unparse date-format from) " "  (f/unparse date-format to))]
    (->> {:type :year :from (str from) :to (str to) :name name}
         persist!)))

(defn reset-db []
  (couch/delete-database db/db)
  (couch/create-database db/db)
  (db/make-db))

;; (sample-db true)   
(defn sample-db
  ([] (sample-db false))
  ([have-extra?]
     (reset-db)
     (make-year (str (t/date-time 2014 6)) (str (t/date-time 2015 6)))
     (make-year (str (t/date-time 2013 6)) (str (t/date-time 2014 5)))
     (let [s (make-student "jim")]
       (when have-extra? (swipe-in (:_id s) (t/now) )))
     (let [s (make-student "steve")]
       (when have-extra? (swipe-in (:_id s) (t/minus (t/now) (t/days 1) (t/hours 5))))))
  )
