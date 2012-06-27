fyrehose internal binary pub/sub protocol
=========================================

in this context, a "client" is a fyrehose-query or fyrehose-journal instance while a 
"backbone"is a fyrehose-backbone instance. everything is big endian.

the basic idea is that every publisher/frontend must sent all incoming messages to a
backbone which will assign a continous sequence number. a publisher can only consider
a message to be "published" after he received it back from the backbone.

the backbone delivers every mesasge to all subscribers but performs no error correction.
a subscriber must therefore check if all preceeding messages have been received (using
the continous sequence number) 

if a client detects message loss, it must send a request for the missing message(s)
to the backbone, which will round-robin forward it to another client that responds
directly to the requester.


RULES:

  1. the client has to send a KEEPALIVE to the backbone at least every one second. the
     backbone has to respond to every KEEPALIVE with a KEEPALIVE-ACK

  2. if a client receives a DELIVER MESSAGE it should check if all messages with
     lower sequence numbers have been received. if not, it may re-request the missing
     message(s) with a REQUEST MESSAGE

  3. if a client receives multiple DELIVER MESSAGE packets with the same sequence
     number it may safely choose any one of them and discard the others

  4. if a client receives a FORWARD MESSAGE packet and stores at least one of the
     requested sequences it must send them to the requester. a client can choose not
     to be sent FORWARD MESSAGE packets by setting the NOJOURNAL flag

  5. the protocol does not guarantee that PUSH MESSAGES are actually received by the
     backbone unless the CONFIRM flag is set




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






