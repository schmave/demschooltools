#!/usr/bin/python

import sys
import urllib

import urllib2

fout = open("email.txt", "wb")

email_text = sys.stdin.read()
fout.write(email_text)
fout.close()


url = "http://people.threeriversvillageschool.org/postEmail"
values = {"email": email_text}

data = urllib.urlencode(values)
req = urllib2.Request(url, data)
try:
    response = urllib2.urlopen(req)
    the_page = response.read()
    open("email_response.txt", "wb").write(the_page)
except urllib2.HTTPError as e:
    open("email_response.txt", "wb").write("Error: \n\n" + str(e))
