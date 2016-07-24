
# overseer

Overseer is a clojure web application which runs on the Jetty server. The site
uses postges for persistence.

## Lein

You will need to get [lein 2](http://leiningen.org/) installed and running. 

To ensure you have the correct version, run: ```lein --version``` and verify
that it is greater than 2.0.

## Postgres

You will need a running [postgres](https://www.postgresql.org/) database on your
system. Once that is complete, create a new database for overseer to use to run.

## Profiles.clj

To run the site locally, you need to configure lein to pass the correct database
connection settings to your running web site. To make this easier, lein allows
for a file to be put in the same folder as the project.clj with machine-specific
settings. 

Make a file called profiles.clj in the base project directory. NEVER ADD IT TO
GIT. Add in the following:

```clojure
{:dev {:repl-options {:init-ns overseer.web}
       :plugins []
       :migratus {:store :database
                  :migration-dir "migrations"
                  :db {:classname "org.postgresql.Driver",
                       :subprotocol "postgresql",
                       :user "USER",
                       :password "PASSWORD",
                       :subname "//localhost:5432/DATABASE"}}
       :dependencies [[clj-webdriver "0.7.2"]
                      [org.apache.httpcomponents/httpclient "4.3.5"]
                      [org.seleniumhq.selenium/selenium-java "2.48.1"]]
       :env {:database-url "postgres://USER:PASSWORD@localhost:5432/DATABASE"
             :admin "web"
             :userpass "web"
             :dev true
             :notify false}}}
```

Replace the following words in your profiles.clj:

* DATABASE - your desired local database name
* USER - your local postgres username
* PASSWORD - your local postgres username's password

Keep in mind that the assumed port number (5432) in the two connection strings
is the postgres default. If your postgres uses a different port, you will need
to change that too.

## NPM

You will need to install [npm](https://www.npmjs.com/).

## Browserify/Watchify

You will need to install watchify and browserify to watch the js files for
changes and build it.

```sh
npm install watchify -g
npm install browserify -g
npm install reactify -g
```

To watch for changes, run: ```make watch```. It should build the app.js file
without errors.

## Running Locally

```sh
$ git clone https://github.com/steveshogren/overseer.git
$ cd overseer
$ lein repl
user=>(def server (overseer.web/-main))
```

Your app should now be running on [localhost:5000](http://localhost:5000/).


License: GPL-3.0+ 
