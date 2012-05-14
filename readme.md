Fyrehose
========

fyrehose is a scala-based, clusterable pub/sub daemon designed to stream json events. 
it allows for server-side history replay, event filtering and a few more advanced queries.


Synopsis
--------

Fyrehose opens up a tcp (and optionally udp) port to which you stream
one event (an arbitrary json object/hash) per packet/message. A messages
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



Fyerhose Query Language
-----------------------

command order within a query is not significannt.

    stream()   
      +only(KEY1,KEY2...)

    count()
    sum(KEY)
    average(KEY)
      +interval(SECONDS)
      +moving_average(SECONDS)

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


advanced:

    pipeline() / execute()


examples:

    filter(channel = 'dawanda-firehose') since(0) until(now)
    filter(channel = 'dawanda-firehose') since(now) stream()
    filter(channel & 'dawanda-firehose','dawanda-searchfeed') since(now) stream()
    filter(_channel = 'dawanda-tap') filter(q_params.page > 150) since(0) stream()" 




JSON Format
-----------

fyerhose know three special json keys/fields:

  _time

    timestamp at which the event was received.
    will be automatically added if not set.


  _eid

    unique event-id. 
    will be automatically added if not set.


  _volatile

    publish, but do not log this event



Usage
-----

    usage: fyerhose [options]
      
      -t, --listen-tcp 

        listen for clients on this tcp address


      -u, --listen-udp

        listen for incoming events on this address


      -p, --path

        path to datastore (default: /tmp/fyerhose/)


      -x, --cluster

         address of the next upstream node (pull)



Advanced / Hacking
------------------

  format: ![hello_args](whitespace/newline)

  format: /![^ ]*[ \n]+/


### bson mode

to enable bson mode, you have to initiate the connection with this: 

    !bson


### keepalive mode:

to enable keepalive mode, you have to initiate the connection with
this: 

    !json;keepalive


if in keepalive mode, the connection wont be closed after the query 
completes. instead the server will sent this line:

    !keepalive \n



### pipelining

if you have multiple queries over common time-ranges and common filters
they can be pipelined to reduce reponse time. pipelining is only available 
in keepalive mode and stream()-queries can't be pipelined.

to pipeline multiple queries send your queries one after another but 
add a "pipeline()" to every query. the server will not execute them yet, 
but respond with the number of queries in the pipeline.

after you added all queries, send a line containing only "execute()". 
this will block the connection until all pipelined queries have completed. 

you then have to re-send all pipelined queries (but this time without the 
pipeline()) to retrieve the results. when re-sending the queries the order 
does not matter.

example:

    count() filter(fu = 'bar1') since(0) until(now) pipline()
    >> 1

    count() filter(fu = 'bar2') since(0) until(now) pipline()
    >> 2

    count() filter(fu = 'bar3') since(0) until(now) pipline()
    >> 3

    execute()
    >> 3

    count() filter(fu = 'bar1') since(0) until(now)
    >> 43534667

    count() filter(fu = 'bar2') since(0) until(now)
    >> 57567456

    count() filter(fu = 'bar3') since(0) until(now)
    >> 34523647
