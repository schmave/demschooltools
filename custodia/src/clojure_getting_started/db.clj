(ns clojure-getting-started.db)

(def db (or (System/getenv "DATABASE_URL")
            "postgresql://localhost:5432/shouter"
            (System/getenv "HEROKU_POSTGRESQL_AMBER_URL")
            "postgresql://localhost:5432/shouter"))
