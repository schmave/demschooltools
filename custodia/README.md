# Overseer

Overseer is a clojure web application which runs on the Jetty server. The site
uses postgres for persistence.

## Frontend

You will need to install [Node.js and npm](https://nodejs.org/en/download/releases/).

### Running Locally

    cd custodia
    npm install
    make watch

## Backend

You will need to get [lein 2](http://leiningen.org/) installed and running.

To ensure you have the correct version, run: `lein --version` and verify
that it is greater than 2.0.

### Postgres

You will need a running [postgres](https://www.postgresql.org/) database on your
system. Once that is complete, create a new database for overseer to use to run.

### Profiles.clj

To run the site locally, you need to configure lein to pass the correct database
connection settings to your running web site. To make this easier, lein allows
for a file to be put in the same folder as the project.clj with machine-specific
settings.

Make a file called profiles.clj in the base project directory. NEVER ADD IT TO
GIT. Add in the following:

```clojure
{
  :dev {:repl-options {:init-ns overseer.web}
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
        :plugins [[com.jakemccrary/lein-test-refresh "0.23.0"]
                  [venantius/ultra "0.5.4"]]
        :env {:database-url "postgres://USER:PASSWORD@localhost:5432/DATABASE"
             :adminpass "web"
             :userpass "web"
             :host "0.0.0.0"
             :port "5000"
             :dev "true"
             :migratedb "true"
             :notify "false"
           }
        :jvm-opts ["-Duser.timezone=GMT"]
  }
}
```

Replace the following words in your profiles.clj:

- DATABASE - your desired local database name
- USER - your local postgres username
- PASSWORD - your local postgres username's password

Keep in mind that the assumed port number (5432) in the two connection strings
is the postgres default. If your postgres uses a different port, you will need
to change that too.

When starting with a new database, or when you want to "reset" your current
database, set the key `:newdb` to `true` before you start the
application. That will cause the database to be recreated empty, then some
sample data be put inside it. CAUTION: leaving it set to `true` will cause
it to drop EVERY TIME. You might want it true for the first time, then
immediately set it to `false` after the application starts.

If you want to apply a new migration to an existing database, set the key
`:migratedb` to `true`.

The application also sets up five default users for local testing. They are
"admin", "super", "admin2", "user", and "demo". They all have the password
"web". Typically, you will use "super" for local testing.
