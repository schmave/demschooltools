
# Overseer

Overseer is a clojure web application which runs on the Jetty server. The site
uses postgres for persistence.

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
             :migratedb false
             :notify false}}}
```

Replace the following words in your profiles.clj:

* DATABASE - your desired local database name
* USER - your local postgres username
* PASSWORD - your local postgres username's password

Keep in mind that the assumed port number (5432) in the two connection strings
is the postgres default. If your postgres uses a different port, you will need
to change that too.

When starting with a new database, or when you want to "reset" your current
database, set the key ```:migratedb``` to ```true``` before you start the
application. That will cause the database to be recreated empty, then some
sample data be put inside it. CAUTION: leaving it set to ```true``` will cause
it to drop EVERY TIME. You might want it true for the first time, then
immediately set it to ```false``` after the application starts.

The application also sets up five default users for local testing. They are
"admin", "super", "admin2", "user", and "demo". They all have the password
"web". Typically, you will use "super" for local testing.

## NPM

You will need to install [Node.js and npm](https://nodejs.org/en/download/releases/).
This project uses a package (npm-shrinkwrap) that is incompatible with npm
version >= 3, so install a 4.x version of Node.js 4.x. Node.js
version 6.x will not work.

## Running Locally

```sh
$ git clone https://github.com/steveshogren/overseer.git
$ cd overseer
$ npm install
```

To build the frontend and serve it while listening for changes,
run ```make watch```

To run the backend, run ```make start```.

If both the frontend and backend are running, the app will be available at
[localhost:5000](http://localhost:5000/).


License: GPL-3.0+
