import psycopg2
import sys
import os
import csv
import datetime
import re

conn = psycopg2.connect("dbname=school_crm user=postgres host=localhost port=5432")
cur = conn.cursor()

cur.execute("set client_encoding to 'latin1'")

cur.execute("SELECT id from tag where title='Current Student' and organization_id=2")
tag_id = int(cur.fetchone()[0])

for line in csv.DictReader(open(sys.argv[1], 'rb')):
    cur.execute("""INSERT into person(
        last_name, first_name, dob, neighborhood, organization_id) VALUES
        (%s, %s, %s, %s, 2) returning person_id""", (
        line["Last Name"].strip(), line["First Name"].strip(), line["DOB"].strip(),
            line["Neighborhood"].strip()))

    person_id = int(cur.fetchone()[0])

    cur.execute("INSERT into person_tag (tag_id, person_id, creator_id) VALUES (%s, %s, 1)",
        (tag_id, person_id))

conn.commit()
cur.close()
conn.close()
