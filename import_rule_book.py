import psycopg2
import sys
import os
import csv
import datetime
import re

conn = psycopg2.connect("dbname=school_crm user=postgres host=localhost")
cur = conn.cursor()

cur.execute("DELETE from entry")
cur.execute("DELETE from section")
cur.execute("DELETE from chapter")
cur.execute("set client_encoding to 'latin1'")

chapter_id = None
section_id = None
rule_content = ""
rule_num = ""
rule_title = ""

for line in open(sys.argv[1], 'r'):
	chapter_match = re.match("Section (\d): (.*)", line)
	rule_match = re.match("\d.(\d\d) (.*)", line)
	
	if chapter_match or rule_match:
		if rule_num and rule_title and rule_content:
			cur.execute("INSERT into entry(num, title, section_id, content) VALUES (%s, %s, %s, %s)", (
				rule_num, rule_title, section_id, rule_content))
		rule_num = rule_title = rule_content = ""
	
	if chapter_match:
		cur.execute("INSERT into chapter (num, title) VALUES (%s, %s) returning id", (
			chapter_match.group(1) + "0", chapter_match.group(2)))
		chapter_id = int(cur.fetchone()[0])
		
		cur.execute("INSERT into section(num, title, chapter_id) VALUES (%s, %s, %s) returning id", (
			"0", "All entries", chapter_id))
		section_id = int(cur.fetchone()[0])
		
	elif rule_match:	
		rule_num = rule_match.group(1)
		rule_title = rule_match.group(2)
	else:
		rule_content += line

conn.commit()
cur.close()
conn.close()
