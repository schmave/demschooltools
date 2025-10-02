# DemSchoolTools

DemSchoolTools is a database that excels at managing Judicial Committee records
for [Sudbury](https://en.wikipedia.org/wiki/Sudbury_school)-inspired schools.
In addition, it does a satisfactory job of tracking people (students, parents,
donors, and others), student attendance, and the management manual.

## Documentation for users

See [the wiki](https://github.com/schmave/demschooltools/wiki/) for more information.

## Running the site locally for development

[Download](https://github.com/schmave/demschooltools/archive/master.zip) the source code, or clone the git repository. `cd` into the root level of the source code.

You'll need to run three separate programs for each of the parts of the site.

### (1 of 3) Play Framework code

1.  [Download](https://openjdk.org/) and install OpenJDK version 11.x. Set the JAVA_HOME environment variable to be the location that you installed it, meaning that $JAVA_HOME/bin/java should be the path to the java binary.

1.  Install [sbt 1.10](https://www.scala-sbt.org/download/). If you use a Mac, I recommend using [homebrew](https://brew.sh/) to install it.

1.  [Download](http://www.postgresql.org/download/) and install PostgreSQL. If you use a Mac, I recommend installing [the "Postgres" app](https://postgresapp.com/) instead.

1.  [Download](https://nodejs.org/en/download/releases/) and install Node v18.

1.  Run `npm install` to install the Javascript libraries.

1.  Start PostgreSQL and create a database named "school_crm". You'll also need to set the password for the user named "postgres" to "123", or change the database username and password in [conf/base.conf](conf/base.conf) and [django/demschooltools/settings.py](django/demschooltools/settings.py).

1.  Install [uv](https://docs.astral.sh/uv/getting-started/installation/#standalone-installer). Once you run it, you'll need to close you terminal window and reopen it before continuing.

1.  Run:

        cd django
        uv run manage.py migrate
        uv run manage.py setup_initial_data
        cd ..

1.  Run `./sbt.sh`, then execute the `eclipse` and `run` command in the sbt console:

        [DemSchoolTools] $ eclipse
        [info] About to create Eclipse project files for your project(s).
        [info] Successfully created Eclipse project files for project(s):
        [info] DemSchoolTools
        [info] authLibrary
        [info] modelsLibrary
        [DemSchoolTools] $ run

1.  Navigate to [http://localhost:9000](http://localhost:9000) in your browser
    and wait while DemSchoolTools is compiled.

1.  Login with Email `admin@asdf.com` and password `nopassword`. You will see
    a page with headings "People", "Attendance", "JC", etc.

### (2 of 3) Django code

Run:

    cd django
    uv run manage.py runserver

### (3 of 3) Custodia frontend code

To enable the Custodia attendance system locally, run:

    cd custodia
    npm install
    npm run watch
