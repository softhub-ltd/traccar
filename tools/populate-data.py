#!/usr/bin/python

# This script populates data to a given device ID.
# Example usage: python populate-data.py 1000 where 1000 is a device Id.

import httplib
import math
import random
import time
import urllib
import sys

if len(sys.argv) != 2:
    sys.stderr.write("usage: {} deviceId".format(sys.argv[0]))
    exit(-1)
deviceUniqueId = sys.argv[1]
print "Populating data to a device with Id = " + deviceUniqueId + " ..."

#deviceUniqueId = '77'  # Make sure the device with this Id exists in Traccar server
#server = 'demo5.traccar.org:5055'
server = 'localhost:5055'
interval = 1  #Interval in seconds
step = 0.001
device_speed = 0
numberOfRequests = 1 #How many times should we post to the device

driver_id = '123456'

waypoints = [
    (48.853780, 2.344347),
    (48.855235, 2.345852),
    (48.857238, 2.347153),
    (48.858509, 2.342563),
    (48.856066, 2.340432),
    (48.854780, 2.342230)
]

points = []

for i in range(0, len(waypoints)):
    (lat1, lon1) = waypoints[i]
    (lat2, lon2) = waypoints[(i + 1) % len(waypoints)]
    length = math.sqrt((lat2 - lat1) ** 2 + (lon2 - lon1) ** 2)
    count = int(math.ceil(length / step))
    for j in range(0, count):
        lat = lat1 + (lat2 - lat1) * j / count
        lon = lon1 + (lon2 - lon1) * j / count
        points.append((lat, lon))


def send(conn, lat, lon, course, speed, alarm, ignition, accuracy, rpm, fuel, driverUniqueId, temp, humid, bat):
    params = (
    ('id', deviceUniqueId), ('timestamp', int(time.time())), ('lat', lat), ('lon', lon), ('bearing', course), ('speed', speed))
    if alarm:
        params = params + (('alarm', 'sos'),)
    if ignition:
        params = params + (('ignition', 'true'),)
    if accuracy:
        params = params + (('accuracy', accuracy),)
    if rpm:
        params = params + (('rpm', rpm),)
    if fuel:
        params = params + (('fuel', fuel),)
    if driverUniqueId:
        params = params + (('driverUniqueId', driverUniqueId),)
    if temp:
        params = params + (('temperature', temp),)
    if humid:
        params = params + (('humidity', humid),)
    if bat:
        params = params + (('batteryLevel', bat),)

    conn.request('GET', '?' + urllib.urlencode(params))
    conn.getresponse().read()


def course(lat1, lon1, lat2, lon2):
    lat1 = lat1 * math.pi / 180
    lon1 = lon1 * math.pi / 180
    lat2 = lat2 * math.pi / 180
    lon2 = lon2 * math.pi / 180
    y = math.sin(lon2 - lon1) * math.cos(lat2)
    x = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(lon2 - lon1)
    return (math.atan2(y, x) % (2 * math.pi)) * 180 / math.pi


index = 0

conn = httplib.HTTPConnection(server)

while index < numberOfRequests:
    (lat1, lon1) = points[index % len(points)]
    (lat2, lon2) = points[(index + 1) % len(points)]
    speed = device_speed if (index % len(points)) != 0 else 0
    alarm = (index % 10) == 0
    ignition = (index % len(points)) != 0
    accuracy = 100 if (index % 10) == 0 else 0
    rpm = random.randint(500, 4000)
    fuel = random.randint(0, 80)
    temp = round(random.uniform(30,60), 2)
    humid = round(random.uniform(20,100), 2)
    bat = round(random.uniform(30,100), 2)

    driverUniqueId = driver_id if (index % len(points)) == 0 else False
    send(conn, lat1, lon1, course(lat1, lon1, lat2, lon2), speed, alarm, ignition, accuracy, rpm, fuel, driverUniqueId,
         temp, humid, bat)
    time.sleep(interval)
    index += 1
