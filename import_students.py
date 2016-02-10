import psycopg2
import sys
import os
import csv
import datetime
import re

ORG_ID=3

conn = psycopg2.connect("dbname=school_crm user=postgres host=localhost port=5432")
cur = conn.cursor()

cur.execute("set client_encoding to 'latin1'")

cur.execute("SELECT id from tag where title='Current Student' and organization_id=%d" % ORG_ID)
tag_id = int(cur.fetchone()[0])

for line in csv.DictReader(open(sys.argv[1], 'rb')):
    keys = ["Last Name", "First Name", "DOB", "Neighborhood"]
    for k in keys:
        if line[k]:
            line[k] = line[k].strip()
        elif k == "Neighborhood":
            line[k] = ""

    cur.execute("""INSERT into person(
        last_name, first_name, dob, neighborhood, organization_id) VALUES
        (%s, %s, %s, %s, %s) returning person_id""", (
        line["Last Name"], line["First Name"], line["DOB"],
            line["Neighborhood"], ORG_ID))

    person_id = int(cur.fetchone()[0])

    cur.execute("INSERT into person_tag (tag_id, person_id) VALUES (%s, %s)",
        (tag_id, person_id))

    cur.execute("INSERT into person_tag_change (tag_id, person_id, creator_id, was_add) VALUES (%s, %s, 1, true)",
        (tag_id, person_id))

conn.commit()
cur.close()
conn.close()
