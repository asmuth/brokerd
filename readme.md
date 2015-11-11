brokerd
========

brokerd is a http based pub/sub daemon. it is quite similar to apache's kafka.


### Curl Example:

in one shell, type this:

    curl localhost:4242/mychannel/subscribe

in another shell run this:

    curl -X POST -d "test" localhost:4242/mychannel


### HTTP API

publish to a channel:

    POST /:channel

subscribe to a channel (responds with http multipart, keep connection open... not supported by all http clients=

    GET /:channel/subscribe

retrieve a message at a specific offset (regular http response)

    GET /:channel/:offset

retrieve the next message after a specific offset (regular http response)

    GET /:channel/:offset/next

retrieve the next :n messages after a specific offset (http multipart response):

    GET /:channel/:offset/next/:n

responds with 'pong'

    GET /ping

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
