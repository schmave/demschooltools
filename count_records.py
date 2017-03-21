import psycopg2
import sys
import os
import csv
import datetime
import re

conn = psycopg2.connect("dbname=school_crm user=evan host=localhost port=5433")
cur = conn.cursor()


print 'Name,Chapter,Section,Entry,Manual change,Person,Tag,Tag Changes,Comments,Attendance Code,Attendance Week,Task,Completed Task,Meeting,Charge,RP complete charge'
for org_id in range(1, 10):
    cur.execute('SELECT name from organization where id=%s', [org_id])
    name = cur.fetchone()[0]
    print name + ',',

    cur.execute('SELECT COUNT(*) from chapter where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from section join chapter on section.chapter_id=chapter.id where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from entry join section on entry.section_id=section.id join chapter on section.chapter_id=chapter.id where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from manual_change join entry on manual_change.entry_id=entry.id join section on entry.section_id=section.id join chapter on section.chapter_id=chapter.id where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from person where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from tag where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from person_tag_change ptc join tag t on t.id=ptc.tag_id where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from comments join person on comments.person_id=person.person_id where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from attendance_code where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from attendance_week join person on attendance_week.person_id=person.person_id where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from task join task_list on task.task_list_id=task_list.id where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from completed_task join task on completed_task.task_id=task.id join task_list on task.task_list_id=task_list.id where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from meeting where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from charge join person on charge.person_id=person.person_id where organization_id=%s', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    cur.execute('SELECT COUNT(*) from charge join person on charge.person_id=person.person_id where organization_id=%s and rp_complete=true', [org_id])
    num = cur.fetchone()[0]
    print str(num) + ',',

    print
