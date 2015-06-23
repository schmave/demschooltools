#DELETE from person where organization_id=4;

import psycopg2
import sys
import os
import csv
import datetime
import re

ORG_ID=4

new_db = psycopg2.connect("dbname=school_crm user=postgres host=localhost port=5432")

new_cur = new_db.cursor()
new_cur.execute("set client_encoding to 'utf8'")

isStudent = True

for row in csv.reader(open("people.csv", "r")):
    if row[0] == "Staff":
        isStudent = False
        continue

    if len(row) == 0:
        print "Empty row"
        continue

    new_cur.execute("""INSERT INTO person(last_name, first_name, organization_id) VALUES
        (%s, %s, %s) returning person_id""", (
            row[1],
            row[0],
            ORG_ID))
    new_id = int(new_cur.fetchone()[0])

    tag_name = "Current Student" if isStudent else "Staff"

    new_cur.execute("""INSERT INTO person_tag(person_id, tag_id) VALUES
        (%s,
        (select id from tag where title=%s and organization_id=%s))""",
        (new_id, tag_name, ORG_ID))


new_db.commit()
new_cur.close()
new_db.close()

