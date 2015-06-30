import psycopg2
import sys
import os
import csv
import datetime
import re

ORG_ID=1

new_db = psycopg2.connect("dbname=school_crm user=postgres host=localhost port=5432")
cur = new_db.cursor()
cur.execute("set client_encoding to 'latin-1'")


def getPersonId(first_name, last_name):
    cur.execute("SELECT person_id from person where first_name=%s and last_name=%s",
        (first_name, last_name))

    result = None
    for record in cur:
        if not result:
            result = record[0]
        else:
            raise "Multiple possibilities for %s,%s" % (first_name, last_name)

    if result:
        return result
    else:
        raise "No person found for %s, %s" % (first_name, last_name)


# Returns tuple (code, start_time, end_time)
def parseTime(start_str, end_str):
    if len(start_str) == 1:
        return (start_str, None, None) # start_str is a code

    input_code = '%I:%M %p'
    start_time = datetime.strptime(start_str, input_code)
    end_time = datetime.strptime(end_str, input_code)

    output_code = '%H:%M:00'
    return (None, start_time.strftime(output_code), end_time.strftime(output_code))


def delete():
    cur.execute("""DELETE from attendance_day ad using person p
        where ad.person_id=p.person_id and p.organization_id=%s""",
        ORG_ID)


if (sys.argv[1] == "delete"):
    delete()
    return

datafile = csv.reader(open(sys.argv[1], 'rb'))
start_date = datetime.strptime(sys.argv[1][:-4], '%Y-%m-%d')

for row in datafile:
    if not row[1]:
        break
    if not row[0]:
        continue

    name = row[0]
    name_splits = name.split(' ')
    first_name = name_splits[0]
    last_name = name_splits[1]

    person_id = getPersonId(first_name, last_name)
    print "%d %s %s" % (person_id, first_name, last_name)

    for i in range(0, 5):
        date = start_date + datetime.timedelta(days=1)
        (code, start_time, end_time) = parseTime(row[i*2+1], row[i*2+2])

        cur.execute("INSERT INTO attendance_day (day, person_id, code, start_time, end_time)
            VALUES (%s, %s, %s, %s, %s)",
            (date, person_id, code, start_time, end_time))


new_db.commit()
new_cur.close()
new_db.close()

