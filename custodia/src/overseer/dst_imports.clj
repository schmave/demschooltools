(ns overseer.dst-imports
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [overseer.commands :as cmd]
            [overseer.database.users :as users]
            [overseer.dates :as dates]
            [overseer.db :as db]
            [overseer.queries :as queries]
            [overseer.records :as r]
            [overseer.roles :as roles]
            [schema.core :as s]
            [yesql.core :refer [defqueries]]
            ))

(def DstStudent
  (merge r/Student {
                    :person_id s/Num
                    :first_name s/Str
                    :display_name s/Str
                    :last_name s/Str
                    }))

(defn get-name-from-dst-fields [student school]
  (if (:use_display_name school)
    (if (> (count (:display_name student)) 0)
      (:display_name student)
      (:first_name student))
    (str (:first_name student) " " (:last_name student))))

(defn ensure-correct-student-name [student school]
  (let [dst-name (get-name-from-dst-fields student school)]
    (if (not (= (:name student) dst-name))
      (db/update! :students (:_id student) {:name dst-name}))))


(defn update-schools-from-dst []
  (let [schools (queries/get-schools-with-dst)
        new-schools (filter #(= (:_id %) nil) schools)]
    (run! (fn [school]
            (db/persist! {:type :schools :name (:name school) :_id (:id school)}))
          new-schools)
    (run! (fn [school]
            (users/make-user (str (:short_name school) "-admin") (env :adminpass) #{roles/admin roles/user} (:id school))
            (users/make-user (str (:short_name school)) (env :userpass) #{roles/user} (:id school)))
          schools)))


(defn bulk-update-student-names [students
                                 school]
  (run! #(ensure-correct-student-name % school) students))


(s/defn create-dst-student [name :- s/Str dst-id :- s/Num school-id :- s/Num]
  (let [new-el {:type :students
                :name name
                :school_id school-id
                :dst_id dst-id
                :start_date nil
                :guardian_email nil
                :olderdate nil
                :is_teacher false
                :show_as_absent nil}]
    (db/persist! new-el)))


(s/defn bulk-insert-new-students [dst-students class-id :- s/Num school]
  (run! (fn [dst]
          (let [name (get-name-from-dst-fields dst school)
                dst-id (:person_id dst)
                ;; _ (pp/pprint {:name name :dst_id dst-id :school school})
                {student-id :_id} (create-dst-student name dst-id (:_id school))]
            (cmd/add-student-to-class student-id class-id)))
        dst-students))

(s/defn ensure-existing-students-in-class
  [student-ids :- [s/Num] class-id :- s/Num school-id :- s/Num]
  (let [students-in-class (->> (queries/get-all-classes-and-students school-id)
                               :classes
                               (filter #(= (:_id %) class-id))
                               first
                               :students
                               (map :student_id)
                               set)
        students-not-in-class (set/difference (set student-ids) students-in-class)
        students-to-remove (set/difference students-in-class (set student-ids))]
    (run! (fn [student-id]
            (cmd/add-student-to-class student-id class-id))
          students-not-in-class)
    (run! (fn [student-id]
            (cmd/remove-student-from-class student-id class-id))
          students-to-remove)))

(defn get-start-of-year []
  (let [now (t/now)
        year-adjustment (if (< (t/month now) 8) -1 0)]
    (t/date-time (+ year-adjustment (t/year now)) 8 1)))

(defn update-all-students-from-dst [school]
  (let [school-id (:_id school)
        students (queries/get-students-with-dst school-id)
        new-students (filter #(= nil (:dst_id %)) students)
        existing-students (filter #(not= nil (:dst_id %)) students)
        start-of-year (get-start-of-year)
        class-name (str (t/year start-of-year) "-" (+ 1 (t/year start-of-year)))
        end-of-year (t/date-time (+ 1 (t/year start-of-year)) 7 31)
        _ (cmd/make-class class-name start-of-year end-of-year school-id)
        {class-id :_id} (queries/get-class-by-name class-name school-id)]
    (cmd/activate-class class-id school-id)

                                        ; Create student records for people who don't exist
                                        ; and add them to a class
    (bulk-insert-new-students new-students class-id school)
    (ensure-existing-students-in-class (map :_id existing-students) class-id school-id)
    (bulk-update-student-names existing-students school)
    ))

(defn update-from-dst []
  (update-schools-from-dst)
  (run! (fn [school]
          (update-all-students-from-dst school))
        (queries/get-schools-with-dst)))
