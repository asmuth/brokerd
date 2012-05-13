Fyerhose
========

fyerhose is a simple, clusterable pubsub daemon that streams json events. 
it allows for server-side history replay and event filtering.


Synopsis:
---------

Fyerhose opens up a tcp (and optionally udp) port to which you stream
one event (an arbitrary json object/hash) per packet/message. Messages
starting with an ASCII bang ("!") are interpreted as queries:

add a few example events:

    echo '{ "action": "signup", "referrer": "ref1" }' | nc localhost 2323
    echo '{ "action": "signup", "referrer": "ref2" }' | nc localhost 2323
    echo '{ "action": "signup", "referrer": "ref3" }' | nc localhost 2323


get the last 60 seconds of signups
 
    echo "! since(-60) until(now) filter(action = 'signup')" | nc localhost 2323


get all signups from ref2 from start of recording till now:
 
    echo "! since(0) until(now) filter(action = 'signup') filter(referrer = 'ref1)" | nc localhost 2323


subscribe to all signups from now on
 
    echo "! since(now) stream() filter(action = 'signup')" | nc localhost 2323



Fyerhose Query Language
-----------------------

command order within a query is not significannt.

#   filter(KEY = VALUE)
#   filter(KEY < MAX)
#   filter(KEY > MIN)
#   filter(KEY ~ MIN-MAX)
#   filter(KEY & ONE,TWO,THREE...)
#   filter(KEY)

#   since(TIMESTAMP)
#   since(-SECONDS)
#   since(now)

#   until(TIMESTAMP)
#   until(-SECONDS)
#   until(now)
#   stream()

#   confirm()


examples:

#   filter(channel = 'dawanda-firehose') since(0) until(now)
#   filter(channel = 'dawanda-firehose') since(now) stream()
#   filter(channel & 'dawanda-firehose','dawanda-searchfeed') since(now) stream()
#   filter(_channel = 'dawanda-tap') filter(q_params.page > 150) since(0) stream()" 



---



    usage: fyerhose [options]
      
      -t, --listen-tcp 

        listen for clients on this tcp address


      -u, --listen-udp

        listen for incoming events on this address


      -p, --path

        path to datastore (default: /tmp/fyerhose/)


      -b, --binary-log

        write the log as bson instead of json


      -x, --upstream

         comma-seperated list of other nodes in the cluster
      



JSON ATTRIBUTES:

  _channel
   
    channel on which the event shoud be broadcasted


  _time

    timestamp at which the event was received.
    will be automatically added if not set.


  _eid

    unique event-id. 
    will be automatically added if not set.


  _request_id / _response_id

    if request id is set, the event with a matching 
    response_id will only be delivered to the requester
    (unless request_timeout was reached, then it will
    be delivered to everybody)


  _nolog

    do not log this event


