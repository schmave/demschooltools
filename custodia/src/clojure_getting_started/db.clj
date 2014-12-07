(ns clojure-getting-started.db
  (:require [carica.core :as c]))
;; http://127.0.0.1:5984/

#_(def db (assoc (cemerick.url/url (str "https://"
                                        (c/config :db :user)
                                        ".cloudant.com/")
                                   (c/config :db :name))
            :username (c/config :db :user)
            :password (c/config :db :password)))
(def db (cemerick.url/url "http://127.0.0.1:5984" (c/config :db :name)))

