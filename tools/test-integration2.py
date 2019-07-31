#!/usr/bin/python

import json
import os
import socket
import sys
import time
import urllib
import urllib2
import xml.etree.ElementTree

messages = {
    'h02': '*HQ,777,V1,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFF,460,00,10342,4283,05,265,52#',
}

baseUrl = 'http://localhost:8082'
user = {'email': 'hemed_ali@hotmail.com', 'password': 'batoji'}

debug = '-v' in sys.argv


def load_ports():
    ports = {}
    dir = os.path.dirname(os.path.abspath(__file__))
    root = xml.etree.ElementTree.parse(dir + '/../setup/default.xml').getroot()
    for entry in root.findall('entry'):
        key = entry.attrib['key']
        if key.endswith('.port'):
            ports[key[:-5]] = int(entry.text)
    if debug:
        print '\nports: %s\n' % repr(ports)
    return ports


def login():
    request = urllib2.Request(baseUrl + '/api/session')
    response = urllib2.urlopen(request, urllib.urlencode(user))
    if debug:
        print '\nlogin: %s\n' % repr(json.load(response))
    return response.headers.get('Set-Cookie')


def remove_devices(cookie):
    request = urllib2.Request(baseUrl + '/api/devices')
    request.add_header('Cookie', cookie)
    response = urllib2.urlopen(request)
    data = json.load(response)
    if debug:
        print '\ndevices: %s\n' % repr(data)
    for device in data:
        request = urllib2.Request(baseUrl + '/api/devices/' + str(device['id']))
        request.add_header('Cookie', cookie)
        request.get_method = lambda: 'DELETE'
        response = urllib2.urlopen(request)


def add_device(cookie, unique_id):
    request = urllib2.Request(baseUrl + '/api/devices')
    request.add_header('Cookie', cookie)
    request.add_header('Content-Type', 'application/json')
    device = {'name': unique_id, 'uniqueId': unique_id}
    response = urllib2.urlopen(request, json.dumps(device))
    data = json.load(response)
    return data['id']


def send_message(port, message):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('127.0.0.1', port))
    s.send(message)
    s.close()


def get_protocols(cookie, device_id):
    params = {'deviceId': device_id, 'from': '2000-01-01T00:00:00.000Z', 'to': '2050-01-01T00:00:00.000Z'}
    request = urllib2.Request(baseUrl + '/api/positions?' + urllib.urlencode(params))
    request.add_header('Cookie', cookie)
    request.add_header('Content-Type', 'application/json')
    request.add_header('Accept', 'application/json')
    response = urllib2.urlopen(request)
    protocols = []
    for position in json.load(response):
        protocols.append(position['protocol'])
    return protocols


ports = load_ports()

cookie = login()
remove_devices(cookie)

devices = {
    '777': add_device(cookie, '777'),
    '5': add_device(cookie, '5'),

}

all = set(ports.keys())
protocols = set(messages.keys())

print 'Total: %d' % len(all)
print 'Missing: %d' % len(all - protocols)
print 'Covered: %d' % len(protocols)

# if all - protocols:
#    print '\nMissing: %s\n' % repr(list((all - protocols)))

for protocol in messages:
    send_message(ports[protocol], messages[protocol])

time.sleep(10)

for device in devices:
    protocols -= set(get_protocols(cookie, devices[device]))

print 'Success: %d' % (len(messages) - len(protocols))
print 'Failed: %d' % len(protocols)

if protocols:
    print '\nFailed: %s' % repr(list(protocols))
