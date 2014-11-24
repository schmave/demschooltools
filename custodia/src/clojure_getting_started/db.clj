(ns clojure-getting-started.db
  (:require [carica.core :as c]))

(def db (assoc (cemerick.url/url (str "https://"
                                      (c/config :db :user)
                                      ".cloudant.com/")
                                 (c/config :db :name))
          :username (c/config :db :user)
          :password (c/config :db :password)))


