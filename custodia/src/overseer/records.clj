(ns overseer.records
  (:require
   [schema.core :as s]
   ))


(def Student {
              :_id s/Num
              :name s/Str
              :display_name s/Str
              })

(def SchoolRecord {:_id s/Num :use_display_name s/Bool})
