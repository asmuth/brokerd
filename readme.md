Fyrehose
========

fyrehose is a scala-based, clusterable pub/sub daemon designed to stream json events. 
it allows for server-side history replay, event filtering and a few more advanced queries.


Synopsis
--------

Fyrehose opens up a tcp (and optionally udp) port to which you stream
one event (an arbitrary json object/hash) per packet/message. Any message
starting with an ASCII bang ("!") will set the connection into query-mode:

add a few example events:

    echo '{ "action": "signup", "referrer": "ref1" }' | nc localhost 2323
    echo '{ "action": "signup", "referrer": "ref2" }' | nc localhost 2323
    echo '{ "action": "signup", "referrer": "ref3" }' | nc localhost 2323


get the last 60 seconds of signups
 
    echo "! stream() filter(action = 'signup') since(-60) until(now)" | nc localhost 2323


subscribe to all signups from ref2 now on
 
    echo "! stream() filter(action = 'signup') filter(referrer = 'ref2') since(now) until(*)" | nc localhost 2323


how many signups in the last hour?
 
    echo "! count() filter(action = 'signup') since(-3600) until(now)" | nc localhost 2323



Usage
-----

    usage: fyerhose [options]
      -t, --listen-tcp <addr>    listen for clients on this tcp address
      -u, --listen-udp <addr>    listen for incoming events on this address
      -p, --path       <path>    path to datastore (default: /tmp/fyerhose/)
      -x, --cluster    <addr>    address of the next upstream node (pull)



JSON Format
-----------

fyerhose know three special json keys/fields:

  **_time** 
  timestamp at which the event was emitted. will be automatically added if not set.

  **_eid** 
  unique event-id. will be automatically added if not set.

  **_volatile** 
  if set, the event will be published, but not logged to disk




Fyerhose Query Language
-----------------------

command/token order within a query is not significant. the order of events in the response is random!

    stream()   
      +only(KEY1,KEY2...)
      +rename(KEY1,KEY2)

    count()
    sum(KEY)
    mean(KEY)
    median(KEY)
    mode(KEY)
    min(KEY)
    max(KEY)
    range(KEY)
      +window(SECONDS)
    
    filter(KEY = VALUE)
    filter(KEY ! VALUE)
    filter(KEY | ONE,TWO,THREE...)

    filter(KEY $ /REGEX/)

    filter(KEY < MAX)
    filter(KEY > MIN)
    filter(KEY ~ MIN-MAX)
    filter(KEY % NUM)
    filter(KEY ^ NUM)
    filter(KEY & NUM)

    filter(KEY any_in ANY_IN,TWO,THREE...)
    filter(KEY all_in ALL_IN,TWO,THREE...)

    filter(KEY)
    filter(!KEY)

    since(TIMESTAMP)
    since(-SECONDS)
    since(now)

    until(TIMESTAMP)
    until(-SECONDS)
    until(now)
    until(*)


examples:

    filter(channel = 'dawanda-firehose') since(0) until(now)
    filter(channel = 'dawanda-firehose') since(now) stream()
    filter(channel & 'dawanda-firehose','dawanda-searchfeed') since(now) stream()
    filter(_channel = 'dawanda-tap') filter(q_params.page > 150) since(0) stream()" 



Advanced / Hacking
------------------

### keepalive mode:

to enable keepalive mode, you have to initiate the connection with
this: 

    !json;keepalive


if in keepalive mode, the connection wont be closed after the query 
completes. instead the server will sent this line:

    !keepalive \n



License
-------

Copyright (c) 2011 Paul Asmuth

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to use, copy and modify copies of the Software, subject 
to the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.