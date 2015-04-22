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

chapter_id = None
section_id = None

last_line_was_rule = False
last_rule_content = ""
last_rule_num = ""

for line in open(sys.argv[1], 'r'):
    chapter_match = re.match("^(\\d) - [\\t ]*(.*)", line)
    section_match = re.match("^[\\t ]*[0-9]-([0-9]+)[\\t ]+(.*)$", line)
    pseudo_section_match = re.match("^[\\t ]+[0-9]-([0-9]+-[0-9]+)[\\t ]*(.*)", line)
    rule_match = re.match("^[0-9]-[0-9]+-([0-9]+)[\\t ]*(.*)", line)

    if last_line_was_rule:
        if not chapter_match and not section_match and not rule_match and not pseudo_section_match:
            last_rule_content += " " + line.strip()
        else:
            cur.execute("""INSERT into entry(num, title, section_id, content)
                VALUES (%s, %s, %s, %s)""", (
                last_rule_num, last_rule_content[:30].strip(),
                    section_id, last_rule_content.strip()))
            last_line_was_rule = False

    if section_match:
        cur.execute("""INSERT into section(num, title, chapter_id) VALUES
            (%s, %s, %s) returning id""", (
            section_match.group(1), section_match.group(2).strip(), chapter_id))
        section_id = int(cur.fetchone()[0])

    if pseudo_section_match:
        cur.execute("""INSERT into section(num, title, chapter_id) VALUES
            (%s, %s, %s) returning id""", (
            pseudo_section_match.group(1), pseudo_section_match.group(2).strip(),
                chapter_id))
        section_id = int(cur.fetchone()[0])

    if rule_match:
        last_rule_num = rule_match.group(1)
        last_rule_content = rule_match.group(2)
        last_line_was_rule = True

    if chapter_match:
        cur.execute("""INSERT into chapter (num, title, organization_id)
            VALUES (%s, %s, %s) returning id""", (
            chapter_match.group(1) + "-", chapter_match.group(2).strip(), ORG_ID))
        chapter_id = int(cur.fetchone()[0])


conn.commit()
cur.close()
conn.close()
