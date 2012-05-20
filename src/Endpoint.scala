package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import java.net._
import java.io._
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.CharBuffer

class Endpoint(multixplex: Multiplex, channel: SocketChannel) extends Actor{

  case class StreamingMode
  case class QueryMode
  var mode = StreamingMode

  var buffer = new Array[Byte](Fyrehose.BUFFER_SIZE_PARSER)
  var buffer_pos = 0


  def act() = { 
    Actor.loop{ react{
      case HangupSig => { println("FIXPAUL: endpoint hangup"); hangup() }
      case buf: Array[Byte] => read(buf)
      case qry: QueryBody   => query(qry)
    }}
  }


  private def query(qry: QueryBody) =
    multixplex.hangup(channel)


  private def write(buf: Array[Byte]) = 
    multixplex.push(channel, buf)


  private def hangup() = {
    channel.close(); exit()
  }


  private def read(buf: Array[Byte]) : Unit = {
    if ((buf.length + buffer_pos) > buffer.length){
      error("endoint parser buffer overflow") 
      return ()
    }

    System.arraycopy(buf, 0, buffer, buffer_pos, buf.length)
    buffer_pos += buf.length

    read_chunked()
  }


  private def read_chunked() : Unit = {
    var escape_seq = false
    var query_seq = false
    var escape_char = 0
    var object_idx = 0
    var trim_pos = 0

    if (buffer(0) == '!')
      query_seq = true
    
    else if (buffer(0) != '{')
      trim_pos = 1

    for(pos <- 1 to buffer_pos){

      if ((trim_pos > 0) && ((buffer(pos) == '{') || (buffer(pos) == '!')))
        return trim_buffer(pos)

      else if (trim_pos > 0)
        trim_pos = pos

      else if ((query_seq) && (buffer(pos) == 10)) {
        read_chunk(pos);
        trim_pos = pos;
      }

      else if (query_seq)
        ()
      
      else if (buffer(pos) == 92)
        escape_seq = true

      else if (((buffer(pos) == 39) || (buffer(pos) == 34)) && !escape_seq && (escape_char == 0))
        escape_char = buffer(pos)

      else if (!escape_seq && (escape_char > 0) && (buffer(pos) == escape_char))
        escape_char = 0

      else if (!escape_seq && (escape_char == 0) && (buffer(pos) == '{'))
        object_idx += 1

      else if (!escape_seq && (escape_char == 0) && (buffer(pos) == '}'))
        object_idx -= 1

      else if (escape_seq && (buffer(pos) != 92))
        escape_seq = false

      if((trim_pos == 0) && (object_idx == -1)){
        read_chunk(pos)
        trim_pos = pos
      }

    }

    if (trim_pos > 0)
      trim_buffer(trim_pos)
    
  }


  private def read_chunk(end_pos: Integer) =
    if (buffer(0) == '!')
      read_query(java.util.Arrays.copyOfRange(buffer, 1, end_pos))
    else if (buffer(0) == '{')
      read_event(java.util.Arrays.copyOfRange(buffer, 0, end_pos + 1))
    else
      Fyrehose.error("something went horribly wrong while parsing")


  private def read_event(buf: Array[Byte]) = 
    Fyrehose.backbone ! new EventBody(buf)


  private def read_query(buf: Array[Byte]) = 
    self ! new QueryBody(buf)


  private def trim_buffer(trim_pos: Integer) = {
    System.arraycopy(buffer, trim_pos, buffer, 0, (buffer.length-trim_pos))
    buffer_pos -= trim_pos

    if (buffer_pos > 0)
      read_chunked()
  }


  private def error(msg: String) = {
    Fyrehose.error("endpoint closed: " + msg)
    write(("{\"error\": \""+msg+"\"}").getBytes)
    hangup()
  }


}