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

  val in_stream  = socket.getInputStream()
  val out_stream = socket.getOutputStream()

  val reactor = actor { loop {
    receive{ 
      case HangupSig => { hangup(); exit() }
      case resp: QueryResponseChunk => write(resp.chunk)
      //case ext: QueryExitSig => finish_query()
      // case TIMEOUT => if (cur_query == null) this ! HangupSig // FIXPAUL implement w/o reactWithin
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
      case e: SocketException => ()
      case e: ParseException => error(e.toString)
    }
  }


  def event(evt_body: EventBody) =
    Fyrehose.backbone.announce(evt_body)


  def query(qry: QueryBody) = try{
    cur_query = QueryParser.parse(qry)
    cur_query ! QueryExecuteSig(reactor)
    Fyrehose.backbone ! cur_query
  } catch {
    case e: ParseException => error(e.toString)
  }


  private def query_finished() = {
    Fyrehose.backbone ! QueryExitSig(cur_query)
    cur_query = null
    
    // if (resp.keepalive unary_!)
     reactor ! HangupSig // FIXPAUL: implement keepalive  
  }


  private def query_abort() = {
    Fyrehose.backbone ! QueryExitSig(cur_query)
    cur_query = null
  }    


  private def write(buf: Array[Byte]) : Unit = try{
    out_stream.write(buf)
  } catch {
    case e: SocketException => reactor ! HangupSig
  }


  private def hangup() = {
    if(cur_query != null)
      query_abort()
    
    Fyrehose.log("connection closed")
    socket.close()
  }


  private def error(msg: String) = {
    Fyrehose.error("endpoint closed: " + msg)
    write(("{\"error\": \""+msg+"\"}").getBytes)
    reactor ! HangupSig
  }


}