(defproject overseer "1.0.0-SNAPSHOT"
  :description "Demo Clojure web app"
  :url "http://overseer.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [org.clojure/tools.nrepl "0.2.7"]
                 [org.clojure/data.json "0.2.5"]
                 [com.cemerick/friend "0.2.1" :exclusions [xerces/xercesImpl]]
                 [org.clojure/tools.trace "0.7.8"]
                 [yesql "0.4.2"]
                 [environ "1.0.0"]
                 [ring/ring-json "0.3.1"]
                 [jdbc-ring-session "0.2"]
                 [sonian/carica "1.1.0" :exclusions [[cheshire]]]
                 [clj-time "0.8.0"]
                 [heroku-database-url-to-jdbc "0.2.2"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [prismatic/schema "0.4.2"]
                 [migratus "0.8.4"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [org.clojure/tools.trace "0.7.8"]
                 [com.ashafa/clutch "0.4.0"]

                 ;; cljs
                 [org.clojure/clojurescript "1.7.170" :scope "provided"]

                 [reagent "0.5.1"]
                 [reagent-forms "0.5.13"]
                 [reagent-utils "0.1.5"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.2.374"]
                 [cljs-ajax "0.5.1"]
                 [markdown-clj "0.9.80"]
                 [org.immutant/web "2.1.1" :exclusions [ch.qos.logback/logback-classic]]
                 ]
  :min-lein-version "2.0.0"
  :plugins [[cider/cider-nrepl "0.10.0-SNAPSHOT"]
            [lein-ring "0.7.0"]
            [lein-cljsbuild "1.1.1"]
            [environ/environ.lein "0.2.1"]]
  :resource-paths ["resources" "target/cljsbuild"]
  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src-cljs"]
     :compiler
     {:output-to "target/cljsbuild/public/js/app.js"
      :output-dir "target/cljsbuild/public/js/out"
      :externs ["react/externs/react.js"]
      :pretty-print true}}}}
  :test-selectors {:default (or (complement :integration)
                                (complement :performance))
                   :integration :integration
                   :all (constantly true)}
  :hooks [environ.leiningen.hooks]
  ;; :main overseer.web
  :uberjar-name "overseer-standalone.jar"
  :profiles {:debug { :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"]}
             :production {:env {:production true}}
             :uberjar {:main overseer.web :aot :all
                       :hooks ['leiningen.cljsbuild]
                       :cljsbuild {:jar true
                                   :builds {:app {:source-paths ["env/prod/cljs"]
                                                  :compiler
                                                  {:optimizations :advanced
                                                   :pretty-print false}}}}
                       }})
