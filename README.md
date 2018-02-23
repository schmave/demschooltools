# DemSchoolTools TEST

new stuff 

DemSchoolTools is a database that excels at managing Judicial Committee records
for [Sudbury](https://en.wikipedia.org/wiki/Sudbury_school)-inspired schools. 
In addition, it does a satisfactory job of tracking people (students, parents, 
donors, and others), student attendance, and the management manual.

## Documentation
See [the wiki](https://github.com/schmave/demschooltools/wiki/) for more information.

## Hacking
1. [Download](https://github.com/schmave/demschooltools/archive/master.zip) 
the source code, or clone the git repository. `cd` into the root level
of the source code.

1. [Download](https://playframework.com/documentation/2.5.x/Installing) 
and install sbt and the Play Framework. You will also need Java 8 if 
you don't have it installed already.

1. [Download](http://www.postgresql.org/download/) and install PostgreSQL, 
including pgAdmin III, their graphical administration tool.

1. [Download](https://nodejs.org/en/download/releases/) and install npm and NodeJS.
  Some versions of npm may not work. NodeJS v4.8.3 and npm v2.15.11 work.
  
1. Run `npm install` to install the Javascript libraries. 

1. Start PostgreSQL and create a database named "school_crm".

1. Set the environment variables APPLICATION_SECRET, GOOGLE_CLIENT_SECRET, 
FACEBOOK_CLIENT_SECRET, and SES_PASSWORD to empty values. You can run the 
"set_keys_blank.sh" script to do this on Mac/Linux.

1. Run sbt "activator" (see [Play documentation](https://playframework.com/documentation/2.5.x/PlayConsole) 
for more info) and execute the "run" command at the activator console.

1. Navigate to [http://localhost:9000](http://localhost:9000) in your browser 
and wait while DemSchoolTools is compiled.

1. When it loads, you should see a message saying 
"Database 'default' needs evolution!". Click "Apply this script now."

1. Open pgAdmin and run this SQL:

        INSERT INTO organization_hosts(host, organization_id) VALUES ('localhost:9000', 1);
        INSERT INTO tag(title, use_student_display, organization_id, show_in_jc) VALUES ('Current Student', true, 1, true);
        INSERT INTO tag(title, use_student_display, organization_id, show_in_jc) VALUES ('Staff', false, 1, true);

1. Disable authentication by adding the following code to the top of `Authenticator.getUsername` in app/controllers/Secured.java:

        if (1 == 1) {
            return "Admin User";
        }

1. Navigate to [http://localhost:9000](http://localhost:9000). You will see 
a page with headings "People", "Attendance", "JC", etc.
