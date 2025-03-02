# DemSchoolTools

DemSchoolTools is a database that excels at managing Judicial Committee records
for [Sudbury](https://en.wikipedia.org/wiki/Sudbury_school)-inspired schools.
In addition, it does a satisfactory job of tracking people (students, parents,
donors, and others), student attendance, and the management manual.

## Documentation

See [the wiki](https://github.com/schmave/demschooltools/wiki/) for more information.

## Hacking

[Download](https://github.com/schmave/demschooltools/archive/master.zip) the source code, or clone the git repository. `cd` into the root level of the source code.

Now that you've got the code, you'll need to run three separate programs for each of the parts of the site.

### Play Framework code

1.  [Download](https://openjdk.org/) and install OpenJDK version 11.x and set the JAVA_HOME environment variable to be the location that you installed it.

1.  Install [sbt 1.10](https://www.scala-sbt.org/download/). If you use a Mac, I recommend using [homebrew](https://brew.sh/) to install it.

1.  [Download](http://www.postgresql.org/download/) and install PostgreSQL.

1.  [Download](https://nodejs.org/en/download/releases/) and install Node v18.

1.  Run `npm install` to install the Javascript libraries.

1.  Start PostgreSQL and create a database named "school_crm". You'll also need to set the password for the user named "postgres" to "123", or change the database username and password in conf/base.conf.

1.  Set the environment variables APPLICATION_SECRET, GOOGLE_CLIENT_SECRET,
    FACEBOOK_CLIENT_SECRET, ROLLBAR_TOKEN, CUSTODIA_PASSWORD, and SES_PASSWORD to empty values. You can run the
    "set_keys_blank.sh" script to do this on Mac/Linux.

1.  Run `./sbt.sh`, then execute the `eclipse` and `run` command in the sbt/play console:

        [DemSchoolTools] $ eclipse
        [info] About to create Eclipse project files for your project(s).
        [info] Successfully created Eclipse project files for project(s):
        [info] DemSchoolTools
        [info] authLibrary
        [info] modelsLibrary
        [DemSchoolTools] $ run

1.  Navigate to [http://localhost:9000](http://localhost:9000) in your browser
    and wait while DemSchoolTools is compiled.

1.  When it loads, you should see a message saying
    "Database 'default' needs evolution!". Click "Apply this script now."

1.  Connect to your Postgres database and run this SQL:

        INSERT INTO organization_hosts(host, organization_id) VALUES ('localhost:9000', 1);
        INSERT INTO tag(title, use_student_display, organization_id, show_in_jc) VALUES ('Current Student', true, 1, true);
        INSERT INTO tag(title, use_student_display, organization_id, show_in_jc) VALUES ('Staff', false, 1, true);

1.  Create a user for logging in:
    Go to `https://www.browserling.com/tools/bcrypt` to encrypt a password
    In pgAdmin run the following query (replacing names and passwords): `INSERT INTO users(email, name, active, email_validated, hashed_password) VALUES ('EMAIL', 'NAME', true, true, 'PASSWORDHERE');`
    In pgAdmin run the following query (replacing the user_id with the user you just created): `INSERT INTO public.user_role (user_id, role) VALUES (USERID, 'all-access');`

1.  Navigate to [http://localhost:9000](http://localhost:9000). You will see
    a page with headings "People", "Attendance", "JC", etc.

### Django code

The Django code uses [uv](https://docs.astral.sh/uv/) to manage its dependencies. I installed uv using using the standalone installer [described here](https://docs.astral.sh/uv/getting-started/installation/#standalone-installer). Once you run it, you'll need to close you terminal window and reopen it before continuing.

Then run:

    uv run manage.py runserver

### Frontend code

    cd custodia
    npm install
    npm run watch
