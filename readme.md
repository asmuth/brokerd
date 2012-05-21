Fyrehose
========

Fyrehose is a scala-based pub/sub daemon that distributes JSON encoded "events". It allows subscribers
to filter their event stream on the server side and to replay their event history.

In simple streaming mode, it handles throughputs beyond 100mbit/s.


Synopsis
--------

To add/publish events, you connect via TCP and send your data as arbitray (possibly nested) 
json-objects. Consequent events/json-objects may be seperated by newline, whitespace, zero-byte
or tab. The only constraint on the format of your json events is, that Fyrehose will add a "_time" 
key containing the timestamp at which the event was received if it doesn't exist already (you can 
also use this to retroactively add events).

To retrieve/subscribe, you also connect with TCP. Every query must be prefixed with an ASCII 
bang ("!") and end with a newline ("\n"). The response consists of one or more newline-
seperated json objects. Unless in keepalive-mode, Fyrehose will close the connection after 
a query has finished. You can only run one query at a time. The order of events within a 
response is random. 


_add a few example events:_

    echo '{ "action": "signup", "referrer": "ref1" }' | nc localhost 2323
    echo '{ "action": "signup", "referrer": "ref2" }' | nc localhost 2323
    echo '{ "action": "signup", "referrer": "ref3" }' | nc localhost 2323


_get the last 60 seconds of signups:_
 
    echo "! stream where(action = 'signup') since(-60) until(now)" | nc localhost 2323


_subscribe to all signups from ref2 now on:_
 
    echo "! stream where(action = 'signup') and where(referrer = 'ref2')" | nc localhost 2323



Usage
-----

    usage: fyerhose [options]
      -l, --listen   <addr>    listen for clients on this tcp address
      -p, --path     <path>    path to store data (default: /tmp/fyerhose/)
      -x, --cluster  <addr>    address of the next upstream node (pull)





Fyerhose Query Language
-----------------------

format / syntax:

    stream
    stream where(...)
    stream where(...) since(...) until(...)
    stream where(...) and/or where(..) since(...) until(...)
    stream where(...) and/or where_not(..) since(...) until(...)


specifing the time range

    since(TIMESTAMP)
    since(-SECONDS)
    since(now)

    until(TIMESTAMP)
    until(-SECONDS)
    until(now)


filters for where / where_not
    
    where(KEY = VALUE)
    where(KEY < MAX)
    where(KEY > MIN)
    where(KEY ~ MIN-MAX)
    where(KEY INCLUDES VALUE)
    where(KEY EXISTS)
    where(KEY % NUM)
    where(KEY ^ NUM)
    where(KEY & NUM)


examples:

    stream since(0) until(now) where(channel = 'dawanda-firehose')
    stream where(channel = 'dawanda-firehose')
    stream where(channel & 'dawanda-firehose','dawanda-searchfeed')
    stream since(0) where(_channel = 'dawanda-tap') where(q_params.page > 150)




Advanced / Hacking
------------------

### keepalive mode:

to enable keepalive mode, send this query: 

    !keepalive \n


if in keepalive mode, the connection wont be closed after the query 
completes. instead the server will sent this line:

    !continue \n



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