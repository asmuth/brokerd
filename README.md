fyrehose
========

fyrehose is a ascii based pub sub protocol, it's simple to understand and can be
parsed efficiently. a lot of the ideas were borrowed from redis.



Protocol
-------

### Message

format:

    @channel\r\n
    $data_length\r\n
    data\r\n

example: send 'hello world' to channel 'fnord'

    @fnord\r\n$11\r\nhello world\r\n


### Subscribe

format:

    +channel\r\n

example: subscribe to channel 'fnord':

    +fnord\r\n


### Unsubscribe

format:

    -channel\r\n

example: unsubscrbie from channel 'fnord':

    -fnord\r\n


### Command

format:

    !command\r\n

commands are user defined and do not have to be implemented at all

