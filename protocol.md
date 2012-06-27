fyrehose backbone pub/sub protocol
==================================

the protocol is designed to be extremly efficent for the central distribution instance
(since this cannot be scaled horizontally) and therefore moves all error handling to
inter-client connections. in a tradeoff for maximum throughput up to one second of data
may be lost in case of a crash/network outage.

the basic idea is that every publisher/client must sent all incoming messages to a
backbone which will assign a continous sequence number. a publisher can only consider
a message to be "published" after he received it back from the backbone.

the backbone delivers every message to all subscribers, but performs no error correction.
a subscriber must therefore check if all preceeding messages have been received (using
the continous sequence number)

if a client detects message loss, it must send a request for the missing message(s)
to the backbone, which will round-robin forward it to another client that responds
directly to the requester.



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



# BASE PACKET

    -------------------------------------------------------------------------------------
    [  TYPE (1 Byte)  |                      DATA (0-65534 Byte)                        ]
    -------------------------------------------------------------------------------------



# 0x1 - DELIVER MESSAGE

    -------------------------------------------------------------------------------------
    [  0x1  |  LENGTH (2 Byte)  |  SEQUENCE (6 Byte)  |        DATA (0-65526 Byte)      ]
    -------------------------------------------------------------------------------------



# 0x2 - PUSH MESSAGE

    -------------------------------------------------------------------------------------
    [  0x2  |  LENGTH (2 Byte)  |                  DATA (0-65526 Bytes)                 ]
    -------------------------------------------------------------------------------------



# 0x4 - REQUEST MESSAGE

    -------------------------------------------------------------------------------------
    [  0x4  |  ADDR (4 Byte)  |  PORT (2 Byte) |  FROM_SEQ (6 Byte) |  TO_SEQ (6 Byte)  ]
    -------------------------------------------------------------------------------------



# 0x8 - FORWARD MESSAGE

    -------------------------------------------------------------------------------------
    [  0x8  |  ADDR (4 Byte)  |  PORT (2 Byte) |  FROM_SEQ (6 Byte) |  TO_SEQ (6 Byte)  ]
    -------------------------------------------------------------------------------------




# 0x10 KEEPALIVE

    ---------------------------------------------------------------------------------
    [  TYPE (1 Byte)  |                ADDR (4 Byte)          |    PORT (2 Byte)    ]
    ---------------------------------------------------------------------------------

    example for 127.0.0.1:2323:

    ---------------------------------------------------------------------------------
    [    00000010     |  01111111 00000000 00000000 00000001  |  00001001 00010011  ]
    ---------------------------------------------------------------------------------






