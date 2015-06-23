#DELETE from manual_change mc using entry e, section s, chapter c
#where mc.entry_id=e.id  and s.id=e.section_id and c.id=s.chapter_id and c.organization_id=4;
#
#DELETE from charge ch using entry e, section s, chapter c
#where ch.rule_id=e.id and s.id=e.section_id and c.id=s.chapter_id and c.organization_id=4;
#
#DELETE from charge ch using person p
#where ch.person_id=p.person_id and p.organization_id=4;
#
#
#DELETE from entry e using section s, chapter c
#where s.id=e.section_id and c.id=s.chapter_id and c.organization_id=4;
#
#DELETE from section s using chapter c
#where c.id=s.chapter_id and c.organization_id=4;
#
#DELETE from chapter c where c.organization_id=4;
#
#DELETE from person_tag_change ptc using person p
#where ptc.person_id=p.person_id and p.organization_id=4;
#
#DELETE from person_at_case pac using person p
#where pac.person_id=p.person_id and p.organization_id=4;
#
#DELETE from person_at_meeting pam using person p
#where pam.person_id=p.person_id and p.organization_id=4;
#
#
#DELETE from "case" c using meeting m
#where c.meeting_id = m.id and m.organization_id=4;
#
#DELETE from meeting m
#where m.organization_id=4;
#
#DELETE from comments c using person p
#where c.person_id=p.person_id and p.organization_id=4;

import psycopg2
import sys
import os
import csv
import datetime
import re

ORG_ID=4

old_db = psycopg2.connect("dbname=tcs user=postgres host=localhost port=5432")
new_db = psycopg2.connect("dbname=school_crm user=postgres host=localhost port=5432")

old_cur = old_db.cursor();

new_cur = new_db.cursor()
new_cur.execute("set client_encoding to 'utf8'")

chapter_id = None
section_id = None

last_line_was_rule = False
last_rule_content = ""
last_rule_num = ""


chapter_to_id = {}
section_to_id = {}
ruleid_to_id = {}

def newSection(section, name, chapter, deleted):
    section_str = "%02d" % section

    new_cur.execute("""INSERT into section(num, title, chapter_id, deleted) VALUES
        (%s, %s, %s, %s) returning id""", (
        section_str,
        name,
        chapter_to_id[chapter],
        deleted))

    section_to_id["%d-%d" % (chapter, section)] = int(new_cur.fetchone()[0])

#####################################
# Lawbook
#

old_cur.execute("SELECT * from tbl_categories")
while True:
    row = old_cur.fetchone()
    if not row:
        break;
    id = row[1]

    name = row[2]
    deleted = row[3]

    new_cur.execute("""INSERT into chapter (num, title, organization_id, deleted)
                VALUES (%s, %s, %s, %s) returning id""", (
                id,
                name,
                ORG_ID,
                deleted))

    chapter_to_id[id] = int(new_cur.fetchone()[0])

old_cur.execute("SELECT * from tbl_categories_subs")
while True:
    row = old_cur.fetchone()
    if not row:
        break;
    chapter = row[1]
    section = row[2]

    name = row[3]
    deleted = row[4]

    newSection(section, name, chapter, deleted)

old_cur.execute("SELECT * from tbl_manmanentries")
while True:
    row = old_cur.fetchone()
    if not row:
        break;
    id = str(row[0])

    chapter = row[1]
    section = row[2]
    short_name = row[5]

    entry_num = row[6].split(".")[1]
    text = row[7]
    deleted = row[9]

    section_key = "%d-%d" % (chapter, section)
    if not section_to_id.has_key(section_key):
        newSection(section, "ESM_unknown", chapter, False)
    section_id = section_to_id[section_key]

    text = unicode(text if text else '', encoding='utf-8')
    text = text.strip()
    text = text.replace("\\n", "\n")
    text = text.replace(u"\u00B7", "* ") # middot
    text = text.replace(u"\u2022", "* ") # bullet

    new_cur.execute("""INSERT into entry(num, title, section_id, content, deleted)
        VALUES (%s, %s, %s, %s, %s) returning id""", (
            entry_num,
            short_name,
            section_id,
            text,
            deleted))

new_db.commit()
new_cur.close()
new_db.close()

