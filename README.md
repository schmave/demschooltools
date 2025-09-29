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

1.  When it loads, you should see a message saying
    "Database 'default' needs evolution!". Click "Apply this script now." If errors happen after you click "Apply this script now.", look in the "Play database evolutions troubleshooting" section below for advice.

1.  Wait until you see the message "Unknown organization" in your browser window.

1.  Connect to your Postgres database and run this SQL:

    ```sql
        INSERT INTO organization_hosts(host, organization_id)
            VALUES ('localhost:9000', 1);
        INSERT INTO tag(title, use_student_display, organization_id, show_in_jc, show_in_attendance, show_in_account_balances, show_in_roles)
            VALUES ('Current Student', true, 1, true, true, true, true);
        INSERT INTO tag(title, use_student_display, organization_id, show_in_jc, show_in_attendance, show_in_account_balances, show_in_roles)
            VALUES ('Staff', false, 1, true, true, true, true);

        INSERT INTO users(email, name, active, email_validated,
                        is_staff, is_superuser, hashed_password)
            VALUES ('admin@asdf.com', 'Admin User', true, true, true, true,
                '$2a$10$sHAtPc.yeZg2AWMr7EZZbuu.sYaOPgFsMZiAY62q/URbjMxU3jB.q');

        INSERT INTO user_role (user_id, role)
            SELECT id, 'all-access' from users;

        UPDATE organization set
            show_custodia=true, show_accounting=true,
            enable_case_references=true, show_electronic_signin=true,
            show_roles=true;
    ```

1.  Reload [http://localhost:9000](http://localhost:9000). Login with Email `admin@asdf.com` and password `nopassword`. You will see
    a page with headings "People", "Attendance", "JC", etc.

### (2 of 3) Django code

The Django code uses [uv](https://docs.astral.sh/uv/) to manage its dependencies. I installed uv using using the standalone installer [described here](https://docs.astral.sh/uv/getting-started/installation/#standalone-installer). Once you run it, you'll need to close you terminal window and reopen it before continuing.

Then run:

    uv run manage.py migrate
    uv run manage.py runserver

### (3 of 3) Custodia frontend code

To enable the Custodia attendance system locally, run:

    cd custodia
    npm install
    npm run watch

# Play database evolutions troubleshooting

### 64.sql

If 64.sql fails to apply with the error `role "custodia" already exists`,
do this:

1. Run the remaining "up" lines from 64.sql:

```sql
grant select on all tables in schema public to custodia;

create schema if not exists overseer;
grant all on schema overseer to custodia;
grant all on all tables in schema overseer to custodia;
grant all on all sequences in schema overseer to custodia;

create schema if not exists demo;
grant all on schema demo to custodia;
grant all on all sequences in schema demo to custodia;
grant all on all tables in schema demo to custodia;
```

2. Click "Mark it resolved".

### 102.sql

If 102.sql fails to apply with the error `relation "custodia_swipe" does not exist`, do this:

1. Run these commands:

```
cd django
uv run manage.py migrate
```

2. Run the "up" line from 102.sql:

```sql
create unique index person_swipe_day_empty_out_unique
    on custodia_swipe(person_id, swipe_day) WHERE out_time is null;
```

3. Click "Mark it resolved".
