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

  var buffer = new Array[Byte](4096)
  var buffer_pos = 0


  def act() = { 
    Actor.loop{ react{
      case buf: Array[Byte] => read(buf)
    }}
  }


  private def write(buf: Array[Byte]) = 
    multixplex.push(channel, buf)


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
    var escape_char = 0
    var object_idx = 0
    var trim_pos = 0

    if (buffer(0) != '{')
      return ()

    for(pos <- 1 to buffer_pos){

      if ((trim_pos > 0) && (buffer(pos) == 123))
        return trim_buffer(pos)

      else if (trim_pos > 0)
        trim_pos = pos

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

      println(buffer(pos), object_idx, escape_seq)
    }

    if (trim_pos > 0)
      trim_buffer(trim_pos)
    
  }


  private def read_chunk(end_pos: Integer){
    println("next length: " + end_pos.toString())
    // try{
    //   val event = new Event(buf)
    //   println("received: " + new String(event.bytes))
    // } catch {
    //   case e: com.google.gson.JsonParseException => error("invalid json")
    // }
  }


  private def trim_buffer(trim_pos: Integer){
    println("trim length: " + trim_pos.toString())

    System.arraycopy(buffer, trim_pos, buffer, 0, (buffer.length-trim_pos))
    buffer_pos -= trim_pos

    //println("buffer after: " + new String(buffer))

    read_chunked()
  }


  private def error(msg: String) = {
    Fyrehose.error("endpoint closed: " + msg)
    write(("{\"error\": \""+msg+"\"}").getBytes)
    println("FIXPAUL: close connection")
  }


}