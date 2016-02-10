import sys
import os
import re
import csv

writer = csv.writer(open(sys.argv[2], 'wb'))

writer.writerow(["First Name","Last Name","DOB","Neighborhood"])

for line in open(sys.argv[1], 'rb'):
    splits = re.split('[ \t]+', line.strip())
    if len(splits) > 1 and len(splits) < 4:
        writer.writerow([splits[0], " ".join(splits[1:])])

