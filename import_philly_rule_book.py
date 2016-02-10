import psycopg2
import sys
import os
import csv
import datetime
import re

conn = psycopg2.connect("dbname=school_crm user=postgres host=localhost port=5432")
cur = conn.cursor()

cur.execute("set client_encoding to 'latin1'")

chapter_id = None
section_id = None

for line in open(sys.argv[1], 'r'):
    chapter_match = re.match("PART ([^:]*):[\\t ]*(.*)", line)
    section_match = re.match("^.(\\d)00[\\t ]*(.*)", line)
    rule_match = re.match("^.\\d([^ \\t]*)[\\t ]*(.*)", line)

    if section_match:
        cur.execute("""INSERT into section(num, title, chapter_id) VALUES
            (%s, %s, %s) returning id""", (
            section_match.group(1), section_match.group(2).strip(), chapter_id))
        section_id = int(cur.fetchone()[0])

    if rule_match and (not section_match or section_match.group(1) == "1"):
        rule_num = rule_match.group(1)
        rule_content = rule_match.group(2)
        cur.execute("""INSERT into entry(num, title, section_id, content)
            VALUES (%s, %s, %s, %s)""", (
            rule_num, rule_content[:30].strip(), section_id, rule_content.strip()))

    if chapter_match:
        cur.execute("""INSERT into chapter (num, title, organization_id)
            VALUES (%s, %s, 2) returning id""", (
            chapter_match.group(1), chapter_match.group(2).strip()))
        chapter_id = int(cur.fetchone()[0])


conn.commit()
cur.close()
conn.close()
