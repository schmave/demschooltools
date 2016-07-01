(defproject overseer "1.0.0-SNAPSHOT"
  :description "Demo Clojure web app"
  :url "http://overseer.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.erg/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring/ring-devel "1.4.0"]
                 [org.clojure/data.json "0.2.5"]
                 [com.cemerick/friend "0.2.1" :exclusions [xerces/xercesImpl
                                                           org.clojure/core.cache]]
                 [org.clojure/tools.trace "0.7.8"]
                 [yesql "0.5.1"]
                 [environ "1.0.0"]
                 [ring/ring-json "0.3.1"]
                 [jdbc-ring-session "0.2"]
                 [sonian/carica "1.1.0" :exclusions [[cheshire]]]
                 [clj-time "0.8.0"]
                 [heroku-database-url-to-jdbc "0.2.2"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [prismatic/schema "0.4.2"]
                 [migratus "0.8.13"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.clojure/tools.trace "0.7.8"]
                 [com.ashafa/clutch "0.4.0"]
                 [org.clojure/java.classpath "0.2.2"]
                 ;;[refactor-nrepl "1.2.0"]
                 [alembic "0.3.2"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 ]
  :min-lein-version "2.0.0"
  :plugins [[cider/cider-nrepl "0.10.0-SNAPSHOT"]
            [refactor-nrepl "2.0.0-SNAPSHOT"]
            [lein-ring "0.9.7"]
            [migratus-lein "0.2.6"]
            [environ/environ.lein "0.2.1"]]
  :migratus {:store :database
             :migration-dir "migrations"
             :db {:classname "org.postgresql.Driver",
                  :subprotocol "postgresql",
                  :user "",
                  :password "",
                  :subname "//localhost:5432/DBNAME"}
             }
  :test-selectors {:default (or (complement :integration)
                                (complement :performance))
                   :integration :integration
                   :all (constantly true)}
  :hooks [environ.leiningen.hooks]
  :ring {:handler overseer.web/tapp
         :nrepl {:start? true
                 :port 9998}}
  ;; :main overseer.web
  :uberjar-name "overseer-standalone.jar"
  :profiles {:debug { :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"]}
             :production {:env {:production true}}
             :uberjar {:main overseer.web :aot :all}})
