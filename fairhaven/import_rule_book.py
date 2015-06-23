#DELETE from manual_change mc using entry e, section s, chapter c
#where mc.entry_id=e.id  and s.id=e.section_id and c.id=s.chapter_id and c.organization_id=3;
#
#DELETE from charge ch using entry e, section s, chapter c
#where ch.rule_id=e.id and s.id=e.section_id and c.id=s.chapter_id and c.organization_id=3;
#
#DELETE from entry e using section s, chapter c
#where s.id=e.section_id and c.id=s.chapter_id and c.organization_id=3;
#
#DELETE from section s using chapter c
#where c.id=s.chapter_id and c.organization_id=3;
#
#DELETE from chapter c where c.organization_id=3;
#
#DELETE from person_tag_change ptc using person p
#where ptc.person_id=p.person_id and p.organization_id=3;
#
#DELETE from person_at_case pac using person p
#where pac.person_id=p.person_id and p.organization_id=3;
#
#DELETE from person_at_meeting pam using person p
#where pam.person_id=p.person_id and p.organization_id=3;
#
#
#DELETE from "case" c using meeting m
#where c.meeting_id = m.id and m.organization_id=3;
#
#DELETE from meeting m
#where m.organization_id=3;
#
#DELETE from comments c using person p
#where c.person_id=p.person_id and p.organization_id=3;
#
#DELETE from person where organization_id=3;
#
import psycopg2
import sys
import os
import csv
import datetime
import re

ORG_ID=3

old_db = psycopg2.connect("dbname=fairhaven user=postgres host=localhost port=5432")
new_db = psycopg2.connect("dbname=school_crm user=postgres host=localhost port=5432")

old_cur = old_db.cursor();

new_cur = new_db.cursor()
new_cur.execute("set client_encoding to 'latin1'")

chapter_id = None
section_id = None

last_line_was_rule = False
last_rule_content = ""
last_rule_num = ""


chapter_to_id = {}
section_to_id = {}
ruleid_to_id = {}

#####################################
# Lawbook
#

old_cur.execute("SELECT * from lawbook order by ruleid ASC")
while True:
    row = old_cur.fetchone()
    if not row:
        break;
    id = str(row[0])
    nickname = row[1]
    chapter = row[2]
    section = row[3]
    if row[4]:
        section = row[4]
    description = row[5]
    enact_date = row[6]

    if not nickname:
        nickname = description[:40]

    if not chapter_to_id.has_key(chapter):
        title_splits = chapter.split("-")
        new_cur.execute("""INSERT into chapter (num, title, organization_id)
                    VALUES (%s, %s, %s) returning id""", (
                    title_splits[0].strip() + "-",
                    "".join(title_splits[1:]).strip(),
                    ORG_ID))
        chapter_to_id[chapter] = int(new_cur.fetchone()[0])

    chapter_id = chapter_to_id[chapter]

    if not section_to_id.has_key(section):
        title_splits = section.split(" ")
        num = title_splits[0]
        num_splits = num.split("-")
        num = '-'.join(num_splits[1:])
        title = " ".join(title_splits[1:]).strip()
        new_cur.execute("""INSERT into section(num, title, chapter_id) VALUES
            (%s, %s, %s) returning id""", (
            num,
            title,
            chapter_id))
        section_to_id[section] = int(new_cur.fetchone()[0])

    section_id = section_to_id[section]

    new_cur.execute("""INSERT into entry(num, title, section_id, content)
        VALUES (%s, %s, %s, %s) returning id""", (
            id[-2:],
            nickname,
            section_id,
            description.strip()))
    ruleid_to_id[id] = int(new_cur.fetchone()[0])

    if enact_date:
        new_cur.execute("""INSERT INTO manual_change(entry_id,
            was_deleted, was_created, date_entered, new_title, new_content, new_num)
            VALUES (%s, %s, %s, %s, %s, %s, %s)""", (
                ruleid_to_id[id], False, True, enact_date,
                nickname, description.strip(), id[-2:]))


#####################################
# People
#

old_cur.execute("""SELECT id, addressid, lastname, firstname, dateofbirth,
    personalid, student, staff from tblpeople where personalid != ''""")

personalid_to_id = {}

while True:
    row = old_cur.fetchone()
    if not row:
        break;

    new_cur.execute("""INSERT INTO person(last_name, first_name, dob, organization_id) VALUES
        (%s, %s, %s, %s) returning person_id""", (
            row[2] if row[2] else "",
            row[3] if row[3] else "",
            row[4],
            ORG_ID))
    new_id = personalid_to_id[row[5]] = int(new_cur.fetchone()[0])

    new_cur.execute("""INSERT INTO comments(person_id, user_id, message) VALUES
        (%s, %s, %s)""", (new_id, 1, "Id: " + str(row[0]) + ", Addressid: " + str(row[1])))


    tag_names = { 6 : "Current Student",
                  7: "Staff" }

    for column in range(6,8):
        if row[column]:
            new_cur.execute("""INSERT INTO person_tag(person_id, tag_id) VALUES
                (%s,
                (select id from tag where title=%s and organization_id=%s))""",
                (new_id, tag_names[column], ORG_ID))


#####################################
# Grievances
#

old_cur.execute("""SELECT plaintiffid, ruleid, date, defendantid,
    what_happened, plea, verdict, sentence from grievances
    WHERE defendantid is not null and plaintiffid is not null
    ORDER BY date ASC""")

last_date = None
last_what_happened = None

meeting_id = None
case_id = None
case_counter = None

while True:
    row = old_cur.fetchone()
    if not row:
        break;

    # Make all rule and personal IDs upper case
    plaintiffid = row[0].upper()
    ruleid = str(row[1])
    defendantid = row[3].upper()

    if row[2] != last_date:
        # New meeting
        new_cur.execute("""INSERT INTO meeting(date, organization_id) VALUES (%s, %s)
            returning id""",
            (row[2], ORG_ID))
        meeting_id = int(new_cur.fetchone()[0])

        last_date = row[2]
        last_what_happened = None
        case_counter = 1
        case_id = None

    if not case_id or row[4] != last_what_happened:
        # New case
        new_cur.execute("""INSERT INTO "case"(meeting_id, case_number, findings)
            VALUES (%s, %s, %s) returning id""",
            (meeting_id, str(last_date) + "-" + str(case_counter),
                "--" if not row[4] else row[4] # findings
        ))
        case_id = int(new_cur.fetchone()[0])

        last_what_happened = row[4]
        case_counter += 1

        # record plaintiff as writer
        if personalid_to_id.has_key(plaintiffid):
            new_cur.execute("""INSERT INTO person_at_case(case_id, person_id, role)
                VALUES(%s, %s, %s)""", (case_id, personalid_to_id[plaintiffid], 1))
        else:
            print "%s, %s, %s -- plaintiff unknown" % (row[2], plaintiffid, row[3])

    # New charge
    assert case_id

    sm_decision = None
    if row[5] != "Guilty":
        sm_decision = row[6] if row[6] else "--"

    if personalid_to_id.has_key(defendantid) and ruleid_to_id.has_key(ruleid):
        new_cur.execute("""INSERT INTO charge(person_id, rule_id, plea, resolution_plan,
            referred_to_sm, sm_decision, sm_decision_date, case_id, rp_complete, rp_complete_date)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
            (personalid_to_id[defendantid],
                ruleid_to_id[ruleid],
                "Unknown" if not row[5] else row[5], #plea
                "--" if not row[7] else row[7], #rp
                row[5] != "Guilty", # referred to sm?
                sm_decision,
                None if row[5] == "Guilty" else last_date, # sm decision date
                case_id,
                True,
                last_date))
    else:
        print "Skipping %s, %s, %s, %s because defendant or rule unknown" % (row[2], row[0], row[3], row[1])


new_db.commit()
new_cur.close()
new_db.close()

