# DemSchoolTools

DemSchoolTools is a database that excels at managing Judicial Committee records
for [Sudbury](https://en.wikipedia.org/wiki/Sudbury_school)-inspired schools.
In addition, it does a satisfactory job of tracking people (students, parents,
donors, and others), student attendance, and the management manual.

# Documentation for users

See [the wiki](https://github.com/schmave/demschooltools/wiki/) for more information.

# Running the site locally for development

## Installation

1.  [Download](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html) and install Oracle JDK version 11.x. Set the JAVA_HOME environment variable to be the location that you installed it, meaning that $JAVA_HOME/bin/java (or $JAVA_HOME/bin/java.exe on Windows) should be the path to the java binary.

1.  Install [sbt 1.x](https://www.scala-sbt.org/download/). If you use a Mac, I recommend using [homebrew](https://brew.sh/) to install it.

1.  [Download](http://www.postgresql.org/download/) and install PostgreSQL. If you use a Mac, I recommend installing the ["Postgres" app](https://postgresapp.com/) instead.

1.  [Download](https://nodejs.org/en/download/archive/v22) and install Node v22. If you use Mac or Linux or Windows WSL, I recommend using [nvm](https://github.com/nvm-sh/nvm/blob/master/README.md#about) to download and install it instead of the Node website.

1.  [Download](https://github.com/schmave/demschooltools/archive/master.zip) the source code, or clone the git repository. `cd` into the root level of the source code.

1.  Run `npm install` to install the Javascript libraries.

1.  Start the PostgreSQL server and create a database named "school_crm". You'll also need to set the password for the user named "postgres" to "123", or change the database username and password in [conf/base.conf](conf/base.conf) and [django/demschooltools/settings.py](django/demschooltools/settings.py).

1.  Install [uv](https://docs.astral.sh/uv/getting-started/installation/#standalone-installer). Once you run it, you'll need to close your terminal window and reopen it before continuing.

1.  Run:

        cd django
        uv run manage.py migrate
        uv run manage.py setup_initial_data
        cd ..

    If you see errors when running `manage.py migrate`, [read below](#dealing-with-inconsistentmigrationhistory) for troubleshooting info.

## Running the site

You'll need to run three separate programs for each of the parts of the site.

### (1 of 3) Play Framework code

1.  Run `./sbt.sh`, then execute the `eclipse` and `run` command in the sbt console:

        [DemSchoolTools] $ eclipse
        [info] About to create Eclipse project files for your project(s).
        [info] Successfully created Eclipse project files for project(s):
        [info] DemSchoolTools
        [info] authLibrary
        [info] modelsLibrary
        [DemSchoolTools] $ run

### (2 of 3) Django code

Run:

    cd django
    uv run manage.py migrate
    uv run manage.py runserver

### (3 of 3) Custodia frontend code

To enable the Custodia attendance system locally, run:

    cd custodia
    npm install
    npm run watch

### Opening the site

Once you have all three servers running, you can:

1.  Navigate to [http://localhost:9000](http://localhost:9000) in your browser
    and wait while DemSchoolTools is compiled.

1.  Login with Email `admin@asdf.com` and password `nopassword`.

## Dealing with "InconsistentMigrationHistory"

If you first set up your local development environment before November 2025, you will see this error when you run `uv run manage.py migrate`:

```
django.db.migrations.exceptions.InconsistentMigrationHistory:
    Migration admin.0001_initial is applied before its
    dependency dst.0001_initial on database 'default'.
```

If you're willing to discard your local data, then the easiest way to fix this is to delete your entire school_crm database and start fresh with the migrate and setup_initial_data installation step above. If you want to keep your data, read on.

### Apply all old Play evolutions

First, you need to apply all Play schema changes that existed before they were deleted and replaced with Django ones.

Run `git checkout cadf15b712e4801fa6f1bfd9cd71f100a89b1519`, then run the Play server as described in "1 of 3" above, go to http://localhost:9000 and click "Apply this script now.".

Then quit Play and sbt and run `git checkout main`.

### Tell Django that the Django migrations have already been applied

Connect to the school_crm database and run this SQL statement:

```sql
INSERT INTO django_migrations(app, name, applied) VALUES
    ('dst', '0001_initial', clock_timestamp()),
    ('custodia', '0003_swipe_person_swipe_day_empty_out_unique', clock_timestamp());
```

Then you should be able to run `uv run manage.py migrate` without errors.
