(defproject overseer "1.0.0-SNAPSHOT"
  :description "Demo Clojure web app"
  :url "http://overseer.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.erg/legal/epl-v10.html"}
  :dependencies [
                 [clj-time "0.14.2"]
                 [com.cemerick/friend "0.2.3" :exclusions [xerces/xercesImpl org.clojure/core.cache]]
                 [compojure "1.6.0"]
                 [environ "1.0.2"]
                 [goat "0.1.0-SNAPSHOT"]
                 [heroku-database-url-to-jdbc "0.2.2"]
                 [jdbc-ring-session "1.0"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jmdk/jmxtools com.sun.jmx/jmxri]]
                 [migratus "0.8.13"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/java.classpath "0.2.3"]
                 [org.clojure/java.jdbc "0.7.5"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.trace "0.7.9"]
                 [org.clojure/tools.trace "0.7.9"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]
                 [prismatic/schema "1.1.7"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-devel "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-json "0.4.0"]
                 [stencil "0.5.0"]
                 [yesql "0.5.3"]
                 ]
  :min-lein-version "2.8.1"
  :plugins [
            [lein-ring "0.9.7"]
            [migratus-lein "0.2.6"]
            [environ/environ.lein "0.2.1"]
            [lein-environ "1.1.0"]]
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
