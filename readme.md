Fyrehose
========

Fyrehose is a scala-based pub/sub daemon for JSON messages. It allows subscribers to filter
messages on the server side and to replay their message history.


**Synopsis / Example:**

_publish a few example messages:_

    echo '{ "action": "signup", "referrer": "ref1" }' | nc -w0 localhost 2323
    echo '{ "action": "signup", "referrer": "ref2" }' | nc -w0 localhost 2323
    echo '{ "action": "signup", "referrer": "ref3" }' | nc -w0 localhost 2323


_subscribe to all signups where referrer matches /^ref/ from now on:_

    echo "stream where action = 'signup' and where referrer = /^ref/" | nc -w0 localhost 2323

_get the last 5 minutes of signups:_

    echo "stream where action = 'signup' since -5min until now" | nc -w0 localhost 2323



Documentation
-------------

To publish a message, open a TCP connection and just send your message as a JSON object.
Subsequent messages may be seperated by newline, whitespace, zero-byte or tab.

The only constraint on the message's schema is that fyrehose will always add a `_time` key
containing the unix timestamp at which the message was received to the root object unless it
exists already (you can also use this to retroactively add messages). Nested messages/objects
are allowed.

To subscribe to messages, send your query over the same connection. Every query must end
with a newline ("\n"). The response consists of one or more newline-seperated json objects.
The order of messages within a response is not guaranteed to be chronological.

You can run multiple queries at the same time. You can also publish messages while queries are
running. Unless in keepalive-mode, Fyrehose will close the connection after all queries have
finished.

### Fyrehose Query Language

keywords / commands:

    STREAM
    COUNT
    SUM
    GROUP
    INFO


specifing the time range

    SINCE [timestamp]
    SINCE -[seconds]
    SINCE NOW

    UNTIL [timestamp]
    UNTIL -[seconds]
    UNTIL NOW
    UNTIL STREAM


filters for where / where_not

    WHERE [key] = "[value]"
    WHERE [key] = /[regex]/
    WHERE [key]  = [other_key]

    WHERE [key] < [max]
    WHERE [key] > [min]
    WHERE [key] % [mod]

    WHERE [key] EXISTS
    WHERE [key] INCLUDES "[value]"


examples:

    stream
    stream where(channel = 'dawanda-firehose')
    stream where_not(params.query = /^fnord/) or where(params.important = true)
    stream since(0) until(now) where(channel = 'dawanda-firehose')
    stream since(-12h) where(channel = 'dawanda-tap') and where(q_params.page > 150)



**KEYS**: non-enclosed strings are treated as keys. you can descend into objects using the dot (`.`) operator. (e.g. params.user.first_name). if your keys contain dots, you must escape dots in your keys with a with a leading backslash. keys must not start with a number or dash symbol.

**STRINGS**: strings can be enclosed in single-quotes `'`, double-quotes `"` or backticks `\``.

**REGEX**: regular expressions must be enclosed in forward slashes `/`. if your regex contains slashes, you must escape them with a leading backslash.

**NUMBERS**: number don't need to be enclosed. numbers may contain a single dot if they are floating-point. numbers must not start with a dot.

**BOOLEAN**: the strings `true` or `false` are treated as boolean values. they must not be enclosed in quotes.

**TIME**: time values may be formatted like positive integers, in which case they are treated as timestamps. time values starting with a minus symbol `-` are treated as "seconds since now", if they end with one of `m`, `h`, `s` or `d` they are respectively treated as minutes, hours, seconds or days since now. the string `now` is also a valid time value.


### Usage

fixpaul: journal (chunksize), idle-timeout, tcp/udp

    usage: fyrehose [options]
      -l, --listen    <addr>    listen for clients on this tcp address
      -p, --path      <path>    path to store data (default: /tmp/fyrehose/)


### Installation

  here be dragons




Advanced / Hacking
------------------

### keepalive mode:

to enable keepalive mode for a connection add `keepalive()` to your query:

    stream [...] keepalive\n


if in keepalive mode, the connection wont be closed after the query
completes. instead the server will sent this message:

    { "_keepalive": 0, "_time": (...) }


the value of the `_keepalive` key indicates the number of running queries,
which will (in this case) always be zero.


even in keepalive mode connections will be closed after a specified idle
timeout. to prevent this and keep the connection opened indefinitely, you
must periodically issue a  `keepalive`. the server will always respond
with the message from above, with the exception that the number of running
queries can be non-zero.

    keepalive\n


### microsecond timestamps

  here be dragons


### strict_mode

  here be dragons




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
