package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

class InboundStream(recv: Endpoint){

  // if safe_mode is disabled, non-json input is silently
  // ignored. safe_mode=false requires strict_mode=true
  var safe_mode = true

  // if strict_mode is enabled all queries must start with
  // an ASCII bang character ('!')
  var strict_mode = false

  var buffer = new Array[Byte](Fyrehose.BUFFER_SIZE_PARSER)
  var buffer_pos = 0


  def set_safe_mode(m: Boolean) =
    safe_mode = m


  def set_strict_mode(m: Boolean) =
    strict_mode = m


  def read(buf: Array[Byte], buf_len: Int) : Unit = {
    if ((buf_len + buffer_pos) > buffer.length){
      throw new ParseException(
        "endoint parser buffer overflow: " +
        new String(java.util.Arrays.copyOfRange(buf, 0, buf_len))
      )
      return ()
    }

    System.arraycopy(buf, 0, buffer, buffer_pos, buf_len)
    buffer_pos += buf_len

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

      else if ((buffer(pos) == 92) && (escape_seq unary_!))
        escape_seq = true

      else if (((buffer(pos) == 39) || (buffer(pos) == 34)) && !escape_seq && (escape_char == 0))
        escape_char = buffer(pos)

      else if (!escape_seq && (escape_char > 0) && (buffer(pos) == escape_char))
        escape_char = 0

      else if (!escape_seq && (escape_char == 0) && (buffer(pos) == '{'))
        object_idx += 1

      else if (!escape_seq && (escape_char == 0) && (buffer(pos) == '}'))
        object_idx -= 1

      //else if (escape_seq && (buffer(pos) != 92))
      else if (escape_seq) 
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
      emit_query(java.util.Arrays.copyOfRange(buffer, 1, end_pos))
    else if (buffer(0) == '{')
      emit_event(java.util.Arrays.copyOfRange(buffer, 0, end_pos))
    else
      throw new ParseException("something went horribly wrong while parsing")


  private def emit_event(buf: Array[Byte]) =
    recv.event(new MessageBody(buf))


  private def emit_query(buf: Array[Byte]) =
    recv.query(new QueryBody(buf ++ "\n".getBytes))


  private def trim_buffer(trim_pos: Integer, check_pos: Integer) = {
    if (safe_mode)
      seek_buffer(check_pos, trim_pos)

    System.arraycopy(buffer, trim_pos, buffer, 0, (buffer.length-trim_pos))
    buffer_pos -= trim_pos

    if (buffer_pos > 0)
      read_chunked()
  }


  private def seek_buffer(from_pos: Integer, until_pos: Integer) : Unit = {
    for (pos <- new Range(from_pos, until_pos, 1)){
      if ((buffer(pos) != 0)  &&
          (buffer(pos) != 9)  &&
          (buffer(pos) != 10) &&
          (buffer(pos) != 13) &&
          (buffer(pos) != 32))
      {
        val buf = java.util.Arrays.copyOfRange(buffer, from_pos, until_pos)
        val buf_str = new String(buf).replace('\n', ' ')

        if (strict_mode)
          throw new ParseException("read invalid data from buffer: " + buf_str)

        else
          return recv.query(new QueryBody(buf_str.getBytes))
      }
    }
  }


}
