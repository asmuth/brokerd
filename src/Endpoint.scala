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

  var safe_mode = true  // FIXPAUL: this should be disable-able for performance
  var keepalive = false // FIXPAUL: this should be disable-able for performance

  var cur_query : Query = null

  var buffer = new Array[Byte](Fyrehose.BUFFER_SIZE_PARSER)
  var buffer_pos = 0


  def act() = { 
    Actor.loop{ react{
      case HangupSig => { println("FIXPAUL: endpoint hangup"); hangup() }
      case buf: Array[Byte] => read(buf)
      case res: QueryResponseChunk => stream_query(res)
      case qry: QueryBody => exec_query(qry)
    }}
  }


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
    var trim_check = 0

    if (buffer(0) == '!')
      query_seq = true
    
    else if (buffer(0) != '{')
      trim_pos = 1

    for(pos <- 1 to buffer_pos){

      if ((trim_pos > 0) && ((buffer(pos) == '{') || (buffer(pos) == '!')))
        return trim_buffer(pos, trim_check)

      else if (trim_pos > 0)
        trim_pos = pos

      else if ((query_seq) && (buffer(pos) == 10)) {
        read_chunk(pos)
        trim_check = pos
        trim_pos = pos
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
        read_chunk(pos + 1)
        trim_check = pos + 1
        trim_pos = pos + 1
      }

    }

    if (trim_pos > 0)
      trim_buffer(trim_pos, trim_check)
    
  }


  private def read_chunk(end_pos: Integer) =
    if (buffer(0) == '!')
      read_query(java.util.Arrays.copyOfRange(buffer, 1, end_pos))
    else if (buffer(0) == '{')
      read_event(java.util.Arrays.copyOfRange(buffer, 0, end_pos))
    else
      Fyrehose.error("something went horribly wrong while parsing")


  private def read_event(buf: Array[Byte]) = 
    Fyrehose.backbone ! new EventBody(buf)


  private def read_query(buf: Array[Byte]) = 
    self ! new QueryBody(buf)


  private def trim_buffer(trim_pos: Integer, check_pos: Integer) = {
    if (safe_mode)
      check_buffer(check_pos, trim_pos) 

    System.arraycopy(buffer, trim_pos, buffer, 0, (buffer.length-trim_pos))
    buffer_pos -= trim_pos

    if (buffer_pos > 0)
      read_chunked()
  }


  private def check_buffer(from_pos: Integer, until_pos: Integer) = {
    for (pos <- new Range(from_pos, until_pos, 1)){
      if ((buffer(pos) != 0)  && 
          (buffer(pos) != 9)  && 
          (buffer(pos) != 10) && 
          (buffer(pos) != 13) && 
          (buffer(pos) != 32))
      {
        val buf = java.util.Arrays.copyOfRange(buffer, from_pos, until_pos)
        error("read invalid data from buffer: " + new String(buf))
      }
    }
  }


  private def exec_query(qry: QueryBody) = try{
    cur_query = QueryParser.parse(qry)
    cur_query.execute(self)
  } catch {
    case e: com.google.gson.JsonParseException => Fyrehose.error("invalid json")
  }


  private def stream_query(resp: QueryResponseChunk) = {
    if (resp.chunk != null)
      write(resp.chunk)

    if (resp.keepalive unary_!){
      cur_query ! HangupSig
      cur_query = null
      multixplex.hangup(channel) // FIXPAUL: implement keepalive  
    }    
  }


  private def error(msg: String) = {
    Fyrehose.error("endpoint closed: " + msg)
    write(("{\"error\": \""+msg+"\"}").getBytes)
    hangup()
  }


}