import psycopg2
import sys

con = psycopg2.connect(database='school_crm', user='postgres', password='123')
cur = con.cursor()

USERS = {
    "Evan Mallory" : 1,
    "Sarah Banach": 2,
    "Jancey" : 3,
    "Jean Marie Pearce" : 4,
    "Chad" : 5,
}

for line in open(sys.argv[1], 'r'):
    splits = line.split('\t')
    if splits[0] == ' Our People Network':
        if (len(splits) < 2):
            print "Weird line: ", line,
            continue
        elif len(splits) == 2:
            splits.append("") # Add an empty comment

        cur.execute("INSERT into person (first_name, notes) VALUES(%s, %s)",
            (splits[1].strip(), splits[2]))
    elif splits[1].find("comment"):
        comment_index = splits[1].find("comment")
        person_name = splits[1][0:comment_index].strip()

        cur.execute("SELECT person_id from person where first_name=%s", [person_name])
        person_id = cur.fetchone()

        comment = splits[2]
        time = splits[3]
        user = splits[4].strip()
        if user not in USERS:
            print "Unknown user: ", user
        cur.execute("INSERT into comments (person_id, user_id, message, created) VALUES (%s, %s, %s, %s)",
            (person_id, USERS[user], comment, time))
    else:
        print "Unknown line ", line,

con.commit()
