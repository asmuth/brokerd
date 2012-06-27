fyrehose internal binary pub/sub protocol
=========================================

in this context, a "client" is a fyrehose frontend daemon instance while a "backbone"
is a fyrehose backbone daemon instance. everything is big endian.



RULES:

  1. the client has to send a KEEPALIVE to the backbone every one second. the
     backbone has to respond to every KEEPALIVE with a KEEPALIVE-ACK

  2. if a client receives a DELIVER MESSAGE it should check if all messages with
     lower sequence numbers have been received. if not, it may re-request the missing
     message(s) with a REQUEST MESSAGE

  3. if a client receives multiple DELIVER MESSAGE packets with the same sequence
     number it may safely choose any one of them and discard the others

  4. if a client receives a FORWARD MESSAGE packet and stores at least one of the 
     requested sequences it must send them to the requester

  5. the protocol does not guarantee that PUSH MESSAGES are actually received by the
     backbone (this needs to be handeled on a different abstraction layer)




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




# KEEPALIVE

    ---------------------------------------------------------------------------------
    [  TYPE (1 Byte)  |                ADDR (4 Byte)          |    PORT (2 Byte)    ]
    ---------------------------------------------------------------------------------

    example for 127.0.0.1:2323:

    ---------------------------------------------------------------------------------
    [    00000010     |  01111111 00000000 00000000 00000001  |  00001001 00010011  ]
    ---------------------------------------------------------------------------------






