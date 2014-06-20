import psycopg2
import sys
import os
import csv
import datetime

conn = psycopg2.connect("dbname=d37bu2tsr2u9lj user=sjaotjnrglevvv host=ec2-23-21-105-133.compute-1.amazonaws.com password=M551hY57NTzeYkn38eA1nac8CV")
cur = conn.cursor()


for line in csv.DictReader(open(sys.argv[1], 'rb')):
	# print line["DateTime"]
	date = line["DateTime"]
	if date:
		date = date.split(' ')[0]
		splits = date.split('/')
		date = datetime.date(int(splits[2]), int(splits[0]), int(splits[1]))
	else:
		date = datetime.date(2013, 5, 19)
	
	name = line["Name"].strip()
	if not name:
		name = line["Email"]
	name_splits = name.split(' ')
	
	ors = ([cur.mogrify("first_name ilike %s", [n]) for n in name_splits] + 
		[cur.mogrify("last_name ilike %s",[n]) for n in name_splits] + 
		[cur.mogrify("email ilike %s",[line["Email"]])])
	ors = " OR ".join(ors)
	
	print "Known people like '" + name + "': "
	
	cur.execute("SELECT person_id, first_name, last_name from person where " + ors)
	for record in cur:
		print "%s. %s %s" % (record[0], record[1], record[2])
		
	entered_id = raw_input()
	
	id = int(entered_id)
	if id < 0:
		cur.execute("INSERT into person (first_name, last_name) VALUES (%s, %s) returning person_id", (
			" ".join(name_splits[:-1]), name_splits[-1]))
		id = int(cur.fetchone()[0])
	else:
		cur.execute("SELECT description from donation where person_id=%s and description like 'Indiegogo --%%'", [id])
		all_results = list(cur)
		if all_results:
			print "ID %d already has an indiegogo donation\n\n" % id
			continue
	print "ID: %d" % id
	
	cur.execute("UPDATE person set email=%s where person_id=%s", (line["Email"], id))
	if line["Address 1"]:
		cur.execute("UPDATE person set address='%s', city='%s', state='%s', zip='%s' WHERE person_id=%d" % (
			line["Address 1"], line["City"], line["State"], line["Zip"], id))

	cur.execute("INSERT into donation (dollar_value, is_cash, description, person_id, date, thanked, indiegogo_reward_given) VALUES (%s, TRUE, '%s', %d, '%s', TRUE, TRUE)" % (
		line["Amount"], "Indiegogo -- " + line["Perk"], id, date))
	conn.commit()
	
cur.close()
conn.close()
