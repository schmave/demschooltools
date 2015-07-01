import logging
import psycopg2
import psycopg2.extras
import sys
import os
import csv
import datetime
import re
import glob

ORG_ID=1

# set logging to DEBUG to see trace from SQL
logging.basicConfig(level=logging.ERROR)
logger = logging.getLogger(__name__)

new_db = psycopg2.connect("dbname=school_crm user=postgres host=localhost port=5432",
    connection_factory=psycopg2.extras.LoggingConnection)
new_db.initialize(logger)
cur = new_db.cursor()#cursor_factory=psycopg2.extras.LoggingCursor)
cur.execute("set client_encoding to 'latin-1'")


def getPersonId(first_name, last_name):
    cur.execute("""SELECT p.person_id from person p join organization o on p.organization_id=o.id
        where p.first_name like %s and p.last_name like %s and o.id=%s""",
        ('%' + first_name + '%', '%' + last_name + '%', ORG_ID))

    result = None
    for record in cur:
        if not result:
            result = record[0]
        else:
            raise Exception("Multiple possibilities for %s,%s" % (first_name, last_name))

    if result:
        return result
    else:
        raise Exception("No person found for %s, %s" % (first_name, last_name))


# Returns tuple (code, start_time, end_time)
def parseTime(start_str, end_str):
    if len(start_str) == 1:
        return (start_str, None, None) # start_str is a code

    try:
        input_code = '%I:%M %p'
        start_time = datetime.datetime.strptime(start_str, input_code)
        end_time = datetime.datetime.strptime(end_str, input_code)
    except ValueError:
        print "Failed to parse %s or %s" % (start_str, end_str)
        return (None, None, None)

    output_code = '%H:%M:00'
    return (None, start_time.strftime(output_code), end_time.strftime(output_code))


def delete():
    cur.execute("""DELETE from attendance_day ad using person p
        where ad.person_id=p.person_id and p.organization_id=%s""",
        [ORG_ID])
    new_db.commit()
    cur.close()


special_names = {
    'Jancey' : ['Jancey', 'Rietmulder-Stone'],
    'Evan' : ['Evan', 'Mallory'],
    'Jean Marie' : ['Jean Marie',  'Pearce'],
    'Marcin' : ['Marcin', 'Jaroszewicz'],
    'Addie Aguilar' : ['Anthony  (Addie)', 'Aguilar'],
    'Damlen Davin': ['Damlen', 'Davis'],
    'Damelin Davis': ['Damlen', 'Davis'],
    'Abby Barry': ['Abigail', 'Barry'],
    'Halaya KMT-Holmes': ['Halaya', 'Holmes'],
}

def processFile(datafile, start_date):
    for row in datafile:
        if not row[0] and not row[1] and not row[2] and not row[3]:
            break
        if not row[0]:
            continue

        name = row[0]
        if special_names.has_key(name):
            first_name = special_names[name][0]
            last_name = special_names[name][1]
        else:
            name_splits = name.split(' ')
            if len(name_splits) < 2:
                raise Exception("Not enough spaces in name %s" % name)
            first_name = name_splits[0]
            last_name = name_splits[1]

        person_id = getPersonId(first_name, last_name)
        print "%s -- %d %s %s" % (start_date, person_id, first_name, last_name)

        for i in range(0, 5):
            date = start_date + datetime.timedelta(days=i)
            (code, start_time, end_time) = parseTime(row[i*2+1], row[i*2+2])

            if code or start_time != "12:00:00" or end_time != "12:00:00":
                cur.execute("""INSERT INTO attendance_day (day, person_id, code, start_time, end_time)
                    VALUES (%s, %s, %s, %s, %s)""",
                    (date, person_id, code, start_time, end_time))
            else:
                cur.execute("""INSERT INTO attendance_day (day, person_id, code, start_time, end_time)
                    VALUES (%s, %s, %s, %s, %s)""",
                    (date, person_id, None, None, None))

        cur.execute("""INSERT INTO attendance_week(monday, person_id, extra_hours)
            VALUES (%s, %s, %s)""",
            (start_date, person_id, 0))

for arg in sys.argv[1:]:
    if len(sys.argv) == 2 and arg == "delete":
        delete()
        break

    if "*" in arg:
        for filename in glob.glob(arg):
            datafile = csv.reader(open(filename, 'rb'))
            try:
                start_date = datetime.datetime.strptime(filename[:-4], '%m-%d-%Y')
                processFile(datafile, start_date)
            except ValueError:
                print "Couldn't parse date for filename %s" % filename
                raise
    else:
        datafile = csv.reader(open(arg, 'rb'))
        start_date = datetime.datetime.strptime(arg[:-4], '%m-%d-%Y')
        processFile(datafile, start_date)

new_db.commit()
cur.close()
new_db.close()

