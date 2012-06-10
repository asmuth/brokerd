package com.paulasmuth.fyrehose

import scala.actors._
import scala.actors.Actor._
import java.io._
import java.net._


object HangupSig{}

class Endpoint(socket: Socket) extends Runnable{

  var safe_mode = true  // FIXPAUL: this should be disable-able for performance
  var keepalive = false // FIXPAUL: this should be disable-able for performance

  var cur_query : Query = null

  Fyrehose.log("connection opened")

  socket.setSoTimeout(Fyrehose.CONN_IDLE_TIMEOUT)

  val in_stream  = socket.getInputStream()
  val out_stream = socket.getOutputStream()

  val reactor = actor { loop {
    receive{ 
      case HangupSig => { hangup() }
      case resp: QueryResponseChunk => write(resp.chunk)
      //case ext: QueryExitSig => finish_query()
    }
  }}


  def run = {
    val parser = new StreamParser(this)
    parser.set_safe_mode(safe_mode)

    var buffer = new Array[Byte](Fyrehose.BUFFER_SIZE_SOCKET)
    var next   = 0

    try{
      while (next > -1){ 
        if(next > 0){ parser.stream(buffer, next) }
        next = in_stream.read(buffer)
      } 
    } catch {
      case e: SocketTimeoutException => error("read timeout", true)
      case e: SocketException => error(e.toString, false)
      case e: ParseException => error(e.toString, true)
      case e: IOException => error(e.toString, false)
    }

    reactor ! HangupSig
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

    // if (resp.keepalive unary_!)
     reactor ! HangupSig // FIXPAUL: implement keepalive  
  }


  private def query_abort() : Unit = {
    if (cur_query == null)
      return ()

    Fyrehose.backbone ! QueryExitSig(cur_query)
    cur_query = null
  }


  private def write(buf: Array[Byte]) : Unit = try{
    out_stream.write(buf)
  } catch {
    case e: SocketException => close_connection()
  }


  private def hangup() : Unit = {
    if(cur_query != null)
      return ()

    Fyrehose.log("connection closed")
    socket.close()
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
