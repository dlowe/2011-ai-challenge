#!/usr/bin/env python

import os, sys
import urllib, urllib2, cookielib
from BeautifulSoup import BeautifulSoup

assert 'AI_USERNAME' in os.environ, "AI_USERNAME must be set in the environment"
assert 'AI_PASSWORD' in os.environ, "AI_PASSWORD must be set in the environment"
assert len(sys.argv) == 2, "usage: upload.py <filename>"

zipbits = open(sys.argv[1]).read()

## set up persistent cookies
cj = cookielib.CookieJar()
opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(cj))
urllib2.install_opener(opener)

## log in
r = urllib2.urlopen('http://aichallenge.org/check_login.php', urllib.urlencode({'username': os.environ['AI_USERNAME'], 'password': os.environ['AI_PASSWORD']}))
assert r.geturl() == 'http://aichallenge.org/index.php', r.geturl()

## fetch submit.php
soup = urllib2.urlopen('http://aichallenge.org/submit.php').read()

## scrape, scrape, scrape
doc = BeautifulSoup(soup)
MAX_FILE_SIZE = str(doc.find(attrs={'name': 'MAX_FILE_SIZE'})['value'])
submit_key = str(doc.find(attrs={'name': 'submit_key'})['value'])

## upload; note that python is missing a battery here ;)
sep  = '--pootpoot'
data = '\r\n'.join([
 '--' + sep, 'Content-Disposition: form-data; name="MAX_FILE_SIZE"', '', MAX_FILE_SIZE,
 '--' + sep, 'Content-Disposition: form-data; name="submit_key"', '', submit_key,
 '--' + sep, 'Content-Disposition: form-data; name="uploadedfile"; filename="pootpoot.zip"',
   'Content-Type: application/zip', '', zipbits,
 '--' + sep + '--', ''
])
request = urllib2.Request('http://aichallenge.org/check_submit.php', data, {'content-type': 'multipart/form-data; boundary=%s' % sep})
r = urllib2.urlopen(request)
assert r.geturl() == 'http://aichallenge.org/check_submit.php', r.geturl()
assert 'Success!' in r.read(), "Doesn't look like success..."
