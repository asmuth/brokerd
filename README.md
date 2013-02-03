fyrehose
========

fyrehose is transactional pub/sub protocol, it's ascii based, simple to understand and can be parsed efficiently. a lot of the ideas were borrowed from redis protocol.


Protocol
-------

### Request / Response

transaction-id is chosen by client.

request format:

    #transaction-id @channel (...)\n

response format (retcode 0 means success):

    #transaction-id $retcode\n


### Data Message

format:

    #transaction-id @channel *len message\n

example: send 'hello world' to channel 'fnord'

    > #12345 @fnord *11 hello world\n
    < #12345 $0


### Transaction Message

format:

    #transaction-id ~channel *len message\n

example: transaction 'hello world' on channel 'fnord'

    > #12345 @fnord *11 hello world\n
    < #12345 $0


### Control Message

format:

    #transaction-id @channel +flags\n

example: subscribe to channel 'fnord':

    > #12345 @fnord +1\n
    < #12345 $0

example: unsubscribe from channel 'fnord':

    > #12345 @fnord +0\n
    < #12345 $0



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
