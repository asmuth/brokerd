fyrehose backbone pub/sub protocol
==================================

the protocol is designed to be extremly efficent for the central distribution instance
(the "backbone") since this cannot be scaled horizontally. it therefore moves all error
handling to the client and inter-client "connections".

the basic idea is that every publisher/client must sent all incoming messages to a
backbone which will assign a continous sequence number. a publisher can only consider
a message to be "published" after he received it back from the backbone.

the backbone delivers every message to all subscribers, but performs no error correction.
a subscriber must therefore check if all preceeding messages have been received (using
the continous sequence number)

if a client detects message loss, it must send a request for the missing message(s)
to the backbone, which will round-robin forward it to another client that responds
directly to the requester.

all messages are delivered via UDP and use network byte order / big endian. the first
byte of each packet contains the packet type.



RULES (CLIENT):

  1. the client has to send a KEEPALIVE to the backbone at least every one second.

  2. if a client receives a DELIVER MESSAGE it should check if all messages with
     lower sequence numbers have been received. if not, it may re-request the missing
     message(s) with a REQUEST MESSAGE

  3. if a client receives multiple DELIVER MESSAGE packets with the same sequence
     number it may safely choose any one of them and discard the others

  4. if a client receives a FORWARD MESSAGE packet and stores at least one of the
     requested sequences it must send them to the requester. a client can choose not
     to be sent FORWARD MESSAGE packets by setting the NOJOURNAL flag

  5. the protocol does not guarantee that PUSH MESSAGES are actually received by the
     backbone (this has to be handeled on a higher abstraction level)

  6. the protocol does not guarantee that there is always at least one client that
     stores each message/sequence. A REQUEST MESSAGE may be un-fullfillable if a 
     message/sequence has been permanently lost.



RULES (BACKBONE)

  1. the backbone has to respond to every KEEPALIVE with a KEEPALIVE-ACK

  2. every client is considered to be a subscriber for the next 5 seconds after
     a KEEPALIVE message has been received (and the NOSUBSCRIBE flag is not set)

  3. if the backbone receives a PUSH MESSAGE packet, it must assign a sequence number
     and send a DELIVER MESSAGE packet to all subscribers

  4. if the backbone receives a REQUEST MESSAGE packet, it must randomly choose one
     subscriber that does not have the NOJOURNAL flag set and send it a FORWARD MESSAGE

  5. PUSH MESSAGE packets must be accepted even from clients that have never sent a
     KEEPALIVE package.



GOTCHAS


  1. in a tradeoff for maximum throughput up to one second of data may be lost in case of a
     crash/network outage. this can be minimized by using a smaller KEEPALIVE interval.

  2. the maximum message size is 65526 byte






### 0x1 - DELIVER MESSAGE

    -------------------------------------------------------------------------------------
    [  0x1  |  LENGTH (2 Byte)  |  SEQUENCE (6 Byte)  |       DATA (0-65526 Byte)       ]
    -------------------------------------------------------------------------------------

    sent either from the backbone to the client or from client to another (following a
    FORWARD MESSAGE packet). when a DELIVER MESSAGE the message (DATA) can be considered
    published




### 0x2 - PUSH MESSAGE

    -------------------------------------------------------------------------------------
    [  0x2  |  LENGTH (2 Byte)  |  UNUSED (6 Byte)   |       DATA (0-65526 Byte)       ]
    -------------------------------------------------------------------------------------

    sent from a client to the backbone to publish a new message. the message (DATA) must
    not be considered published until the corresponding DELIVER MESSAGE was received.




### 0x4 - REQUEST MESSAGE

    -------------------------------------------------------------------------------------
    [  0x4  |  ADDR (4 Byte)  |  PORT (2 Byte) |  FROM_SEQ (6 Byte) |  TO_SEQ (6 Byte)  ]
    -------------------------------------------------------------------------------------

   sent from a client to the backbone after missing messages have been detected. addr and
   port should be the requesters listening udp address and port.




### 0x8 - FORWARD MESSAGE

    -------------------------------------------------------------------------------------
    [  0x8  |  ADDR (4 Byte)  |  PORT (2 Byte) |  FROM_SEQ (6 Byte) |  TO_SEQ (6 Byte)  ]
    -------------------------------------------------------------------------------------

    sent from the backbone to one client after a REQUEST MESSAGE was received. contains
    the requesters udp listening address and ports. if the client can fulfill the request
    it should send the response as DELIVER MESSAGE packets directly to the requester.




### 0x10 KEEPALIVE

    -------------------------------------------------------------------------------------
    [  0x10 |  ADDR (4 Byte)  |  PORT (2 Byte) |  FLAGS (6 Byte)  |   TOKEN (16 Byte)   ]
    -------------------------------------------------------------------------------------

    sent from a client to the backbone to sign-on or keepalive the connection. a client
    will be considered a subscriber by the backbone for the next 5 seconds after a the
    KEEPALIVE packet unless the NOSUBSCRIBE flag is set. the backbone will also forward
    REQUEST MESSAGE packets as FORWARD MESSAGE packets for 5 seconds after the KEEPALIVE
    unless the NOJOURNAL flag was set.

    FLAGS: 0x1 -> NOSUBSCRIBE, 0x2 -> NOJOURNAL




### 0x20 KEEPALIVE-ACK

    -------------------------------------------------------------------------------------
    [  0x20 |                           TOKEN (16 Byte)                                 ]
    -------------------------------------------------------------------------------------

    sent from the backbone to a client after a KEEPALIVE has been received. TOKEN is the
    same as in the original KEEPALIVE packet.


