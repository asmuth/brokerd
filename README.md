brokerd
=======

brokerd is a lightweight message broker ("pub-sub") service. A brokerd instance
manages a number of "channels". Each channel maps to a file on disk and supports
two operations: `append(channel, msg)` and `getnext(channel, offset)`. 

The append operation appends a message at the end of the file. Messages are then
identified by the (logical) file offset at which they were written. The initial
offset for the first message in a channel is zero and then increases monotonically
with each subsequent message.

The getnext operation reads a batch of messages from a channel starting at a given
offset. When consuming messages from a channel the client is responsible for
storing the last offset it has consumed.

If a disk space limit is configured using `--disklimit`, old messages will
eventually be deleted from the beginning of the channel to reclaim space. When a
client tries to read a message (offset) that has been gargbage collected, brokerd
will return the next valid message in the channel. Reading from offset zero is
therefore _always_ a valid operation and returns the first/oldest retained message
from the channel.

[Full Documentation](https://brokerd.org)


Getting Started
---------------

Execute the following command to start brokerd on HTTP port 8080. The messages
will be stored in `/var/brokerd`:

    $ mkdir /var/brokerd
    $ brokerd --datadir /var/brokerd --listen_http localhost:8080

In a shell, start this command to read messages from the "testchan" channel
as they are being written (the command will not return or output anything for
now but that is intended):

    curl -v localhost:8080/testchan/subscribe

Then open another shell and run this command to insert the message "testing"
into our channel:

    curl -X POST -d "testing" localhost:4242/testchan

The output should look similar to this:


HTTP API
--------

The HTTP+JSON API is very simple. Below is a list of all API methods. For more
detailed documentation on the API please [check out the documentation](https://brokerd.org)

    POST /channel/:channel
         Append a message to a channel (the message is the POST body)

     GET /channel/:channel/subscribe
         Subscribe to a channel (returns a HTTP SSE stream of events)
 
     GET /channel/:channel/:offset
         Retrieve a message at a specific offset
 
     GET /channel/:channel/:offset/next
         Retrieve the next message after a specific offset
 
     GET /channel/:channel/:offset/next/:n
         Retrieve the next N messages after a specific offset
 
     GET /stats
         Responds with a list of statistics
 
     GET /serverid
         Returns a unique server ID
 
     GET /ping
         Responds with 'pong'
 

Usage
-----

The brokerd distribution consists of two programs: `brokerd` and `brokerctl`.
The brokerd program is the main server program and the second brokerctl program
is a simple command line client. For more information please
[check out the documentation](https://brokerd.org)

    Usage: $ brokerd [OPTIONS]
       --listen_http <addr>          Listen for HTTP connection on this address
       --datadir <dir>               Set the data directory
       --disklimit <limit>           Delete old messages to keep total size < limit
       --disklimit_channel <limit>   Delete old messages to keep every channel size < limit
       --daemonize                   Daemonize the server
       --pidfile <file>              Write a PID file
       --loglevel <level>            Minimum log level (default: INFO)
       --[no]log_to_syslog           Do[n't] log to syslog
       --[no]log_to_stderr           Do[n't] log to stderr
       -?, --help                    Display this help text and exit
       -V, --version                 Display the version of this binary and exit

    Examples:
       $ brokerd --datadir /var/brokerd --listen_http localhost:8080
       $ brokerd --datadir /var/brokerd --listen_http localhost:8080 --disklimit 20GB


Building
--------

Before we can start we need to install some build dependencies. Currently
you need a modern c++ compiler, libz and autotools.

    # Ubuntu
    $ apt-get install clang make automake autoconf libtool zlib1g-dev

    # OSX
    $ brew install automake autoconf

To build brokerd from a git checkout:

    $ git clone git@github.com:paulasmuth/brokerd.git
    $ cd brokerd
    $ ./autogen.sh
    $ ./configure
    $ make V=1
    $ sudo make install


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

