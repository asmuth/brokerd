brokerd
=======

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

brokerd  is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

FnordMetric is distributed in the hope that it will be useful,but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
FnordMetric. If not, see <http://www.gnu.org/licenses/>.
