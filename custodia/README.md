
# overseer

A barebones Clojure app, which can easily be deployed to Heroku.

## Running Locally

Make sure you have Clojure installed.  Also, install the [Heroku Toolbelt](https://toolbelt.heroku.com/).

```sh
$ git clone https://github.com/heroku/overseer.git
$ cd overseer
$ lein repl
user=> (require 'overseer.web)
user=>(def server (overseer.web/-main))
```

Your app should now be running on [localhost:5000](http://localhost:5000/).

## Profiles.clj

To run the site locally, you need to configure lein to pass the correct database
connection settings to your running web site. To make this easier, lein allows
for a file to be put in the same folder as the project.clj with machine-specific
settings. 

You will want to make a file called profiles.clj in the base project directory.
DO NOT ADD IT TO GIT. Add in the following:

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
             :dev true}}}
```

Replace the following words in your profiles.clj:

* DATABASE - your desired local database name
* USER - your local postgres username
* PASSWORD - your local postgres username's password

Keep in mind that the assumed port number (5432) is the postgres default. If
your postgres uses a different port, you will need to change that too.

