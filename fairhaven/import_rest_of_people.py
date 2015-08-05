#DELETE from comments c using person p
#   where c.person_id=p.person_id and p.person_id>5298 and p.organization_id=3;
#DELETE from person_tag pt using person p
#   where pt.person_id=p.person_id and p.person_id>5298 and p.organization_id=3;
# UPDATE person set family_person_id=null where organization_id=3;
# DELETE FROM person where person_id > 5298 and organization_id=3;

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

TAGS = { 9: "Assembly Parent",
			  10: "Alumnus",
			  11: "Public Assembly Member",
			  12: "FSI Board",
			  13: "Admit: Applying",
			  14: "Admit: Waiting list",
			  15: "Potential donor",
			  16: "Potential staff",
			  19: "Alumni parent",
			  21: "Media",
			  22: "Education", 
			}
			
for name in TAGS.values():
	try:
		new_cur.execute("INSERT INTO tag (title, organization_id) VALUES (%s, %s)",
			(name, ORG_ID))
		new_db.commit()
	except psycopg2.IntegrityError:
		# Tag already exists?
		new_db.rollback()
		pass

old_cur.execute("SELECT * from address order by addressid ASC")
while True:
	address = old_cur.fetchone()
	if not address:
		break
		
	cur = old_db.cursor()
		
	# create family record
	new_cur.execute("""INSERT into person (first_name, last_name, is_family, created, organization_id)
				VALUES (%s, %s, true, %s, %s) returning person_id""", (
				address[18] if address[18] else "",
				address[19] if address[19] else "",
				address[15],
				ORG_ID))
	new_family_id = int(new_cur.fetchone()[0])
	
	# print "\n\nCreated family for addressid %s" % address[0]
	
	address_recorded = False
	address_has_people = False
	
	cur.execute("SELECT * from tblpeople where addressid=%s order by id ASC",
		(address[0],))
	while True:
		person = cur.fetchone()
		if not person:
			break;
			
		address_has_people = True
			
		# print "Processing person %s, %s" % (person[2], person[3])

		# check to see if person exists or not
		new_cur.execute("""SELECT p.person_id from person p join comments c on p.person_id=c.person_id
		   where c.message like '%%Id: %s%%,'""", (person[20],))
		  
		result = new_cur.fetchone()
		existing_person_id = None
		if result:
			existing_person_id = int(result[0])
		
		# create if it doesn't exist
		if not existing_person_id:
			new_cur.execute("""INSERT INTO person(first_name, last_name, dob, organization_id, created) VALUES
				(%s, %s, %s, %s, %s) returning person_id""", (
					person[3] if person[3] else "-No name-",
					person[2] if person[2] else "",
					person[4],
					ORG_ID,
					address[15]))
			existing_person_id = int(new_cur.fetchone()[0])

			new_cur.execute("""INSERT INTO comments(person_id, user_id, message, created) VALUES
				(%s, %s, %s, %s)""", (
				existing_person_id, 
				1, 
				"Id: " + str(person[20]) + ", Addressid: " + str(person[0]),
				'1970-01-01'))
		
		# set family record
		new_cur.execute("UPDATE person set family_person_id=%s where person_id=%s",
			(new_family_id, existing_person_id))
			
		# set tags
		for column in TAGS.keys():
			if person[column]:
				# print "Tagging %s as %s" % (person[3], TAGS[column])
				new_cur.execute("""INSERT INTO person_tag(person_id, tag_id) VALUES
					(%s,
					(select id from tag where title=%s and organization_id=%s))""",
					(existing_person_id, TAGS[column], ORG_ID))
				
		# set address info if no DOB and address info not already set on someone
		if not address_recorded and not person[4]:
			address_recorded = True
			
			email = address[21] if address[21] else ""
			
			new_cur.execute("UPDATE person set (address, city, state, zip, email) = (%s, %s, %s, %s, %s) where person_id=%s",
				(address[3] if address[3] else "",
				address[4] if address[4] else "",
				address[5] if address[5] else "",
				address[6] if address[6] else "",
				email if '\\n' not in email else "",
				existing_person_id))
				
			if '\\n' in email:
				new_cur.execute("UPDATE person set notes=%s where person_id=%s",
					("Multiple emails for this family: " + email.replace('\\n', '\n'), existing_person_id))
			
			# phone and fax
			if address[7]:
				# Multiple phone numbers are listed separated by a \\n. 
				# Would be nice to enter each one separately, but for now, 
				# just give them a semicolon instead
				phone = address[7].replace('\\n', '; ')
				new_cur.execute("INSERT INTO phone_numbers (person_id, comment, \"number\") VALUES (%s, %s, %s)",
					(existing_person_id, "phone", phone))
			
			if address[8]:
				new_cur.execute("INSERT INTO phone_numbers (person_id, comment, \"number\") VALUES (%s, %s, %s)",
					(existing_person_id, "fax", address[8]))
			
			# also create comment for "whereheard" and "comments" from address record
			if address[10]:
				new_cur.execute("INSERT INTO comments (person_id, user_id, message, created) VALUES (%s, 1, %s, %s)",
					(existing_person_id, 
					"Access DB comment: " + address[10],
					address[15]))
			
			if address[11]:
				new_cur.execute("INSERT INTO comments (person_id, user_id, message, created) VALUES (%s, 1, %s, %s)",
					(existing_person_id, 
					"Access DB where heard: " + address[11],
					address[15]))
			
	if address_has_people and not address_recorded:
		print "WARN      No address recorded for addressid %d" % (address[0])


new_db.commit()
new_cur.close()
new_db.close()