package com.paulasmuth.fyrehose

import scala.actors._
import scala.actors.Actor._
import java.io._
import java.net._

object HangupSig{}
object TimeoutSig{}

class Endpoint(socket: Socket) extends Runnable{

  var safe_mode = true  // FIXPAUL: this should be disable-able for performance
  var keepalive = false // FIXPAUL: this should be disable-able for performance

  var cur_query : Query = null
  var idle_status       = true

  socket.setSoTimeout(Fyrehose.CONN_IDLE_TIMEOUT)
  val in_stream  = socket.getInputStream()
  val out_stream = socket.getOutputStream()

  Fyrehose.log("connection opened")

  val reactor = actor { loop {
    receive{ 
      case HangupSig  => { hangup(); exit() }
      case TimeoutSig => timeout()
      case resp: QueryResponseChunk => write(resp.chunk)
      case xsig: QueryExitSig => query_finished()
    }
  }}


  def run = {
    val parser = new StreamParser(this)
    parser.set_safe_mode(safe_mode)

    var buffer = new Array[Byte](Fyrehose.BUFFER_SIZE_SOCKET)
    var next   = 0

    try{
      while ((next > -1) || (idle_status == false)){
        if (next > 0)
          parser.stream(buffer, next)

        next = in_stream.read(buffer)

        if (next == -1){
          idle_status = true
          Thread.sleep(Fyrehose.CONN_IDLE_TIMEOUT)
        }
      }
    } catch {
      case e: SocketTimeoutException => { reactor ! TimeoutSig; run }
      case e: SocketException => close_connection()
      case e: ParseException => error(e.toString, true)
      case e: IOException => error(e.toString, false)
    }

    close_connection()
  }


  def event(ev_body: EventBody) = try{
    Fyrehose.backbone ! new Event(ev_body.raw)
  } catch {
    case e: ParseException => error(e.toString, true)
  }


  def query(qry: QueryBody) = try{
    cur_query = QueryParser.parse(qry)
    cur_query ! QueryExecuteSig(reactor)
    Fyrehose.backbone ! cur_query
  } catch {
    case e: ParseException => error(e.toString, true)
  }


  private def query_finished() : Unit = {
    if (cur_query == null)
      return ()

    Fyrehose.backbone ! QueryExitSig(cur_query)
    cur_query = null

    if (keepalive unary_!)
     reactor ! HangupSig
  }


  private def query_abort() : Unit = {
    if (cur_query == null)
      return ()

    Fyrehose.backbone ! QueryExitSig(cur_query)
    cur_query = null
  }


  private def write(buf: Array[Byte]) : Unit = try{
    idle_status = false
    out_stream.write(buf)
  } catch {
    case e: SocketException => close_connection()
  }


  private def hangup() : Unit = {
    Fyrehose.log("connection closed")
    socket.close()
  }


  private def timeout() : Unit = {
    if(cur_query != null)
      return ()

    error("read timeout", true)
  }


  private def error(msg: String, recoverable: Boolean) = {
    Fyrehose.error("endpoint closed: " + msg)

    if (recoverable)
      write(("{\"error\": \""+msg+"\"}").getBytes) // FIXPAUL

    close_connection()
  }


  private def close_connection() = {
    query_abort()
    reactor ! HangupSig
  }

}
