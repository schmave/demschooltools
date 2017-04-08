# DemSchoolTools

DemSchoolTools is a database that excels at managing Judicial Committee records for [Sudbury](http://sudval.org/)-inspired schools. In addition, it does a satisfactory job of tracking people (students, parents, donors, and others), student attendance, and the management manual.


## Documentation
See [the wiki](https://github.com/schmave/demschooltools/wiki/) for more information.

## Hacking
1. [Download](https://github.com/schmave/demschooltools/archive/master.zip) the source code, or clone the git repository.
2. [Download](https://playframework.com/download) the Play Framework.
3. [Download](http://www.postgresql.org/download/) and install PostgreSQL, including pgAdmin III, their graphical administration tool.
4. Start PostgreSQL and create a database named "school_crm".
5. Set the environment variables APPLICATION_SECRET, GOOGLE_CLIENT_SECRET, FACEBOOK_CLIENT_SECRET, and MANDRILL_API_KEY to empty values. You can run the "set_keys_blank.sh" script to do this on Mac/Linux.
5. Run sbt "activator" (see [Play documentation](https://playframework.com/documentation/2.5.x/PlayConsole) for more info) and execute the "run" command at the activator console.
6. Navigate to [http://localhost:9000](http://localhost:9000) in your browser and wait while DemSchoolTools is compiled.
7. When it loads, you should see a message saying "Database 'default' needs evolution!". Click "Apply this script now."
8. Open pgAdmin and run this SQL:

        INSERT INTO organization_hosts(host, organization_id) VALUES ('localhost:9000', 1);
        INSERT INTO tag(title, use_student_display, organization_id) VALUES ('Current Student', true, 1);
        INSERT INTO tag(title, use_student_display, organization_id) VALUES ('Staff', false, 1);
        INSERT INTO users(email, name, active, email_validated, organization_id) VALUES ('<your email address here>', '<your name>', true, true, null);

9. Navigate to [http://localhost:9000](http://localhost:9000). You will see a login page. Log in with a Google or Facebook account associated with the email address that you put in the users table in the previous step. Once you log in, you will see a page with headings "People", "Attendance", "JC", etc.
