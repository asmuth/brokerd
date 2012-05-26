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

  val parser = new StreamParser(this)
  parser.set_safe_mode(safe_mode)

  Fyrehose.log("connection opened")

  val in_stream  = socket.getInputStream()
  val out_stream = socket.getOutputStream()

  val reactor = actor { loop {
    react{ 
      case HangupSig => { hangup(); exit() }
      case res: QueryResponseChunk => stream_query(res)
      //case ext: QueryExitSig => finish_query()
      // case TIMEOUT => if (cur_query == null) this ! HangupSig // FIXPAUL implement w/o reactWithin
    }
  }}


  def run = {
    var buffer = new Array[Byte](Fyrehose.BUFFER_SIZE_SOCKET)

    try{
      while(in_stream.read(buffer) > -1){ 
        read(buffer) 
      }  
    } catch {
      case e: SocketException => ()
    }
  }


  def event(evt_body: EventBody) =
    Fyrehose.backbone ! evt_body


  def query(qry: QueryBody) = try{
    cur_query = QueryParser.parse(qry)
    cur_query ! QueryExecuteSig(reactor)
    Fyrehose.backbone ! cur_query
  } catch {
    case e: ParseException => error(e.toString)
  }


  private def read(buf: Array[Byte]) : Unit = try{
    parser.stream(buf)
  } catch {
    case e: ParseException => error(e.toString)
  }


  private def write(buf: Array[Byte]) = try{
    out_stream.write(buf)
  } catch {
    case e: SocketException => reactor ! HangupSig
  }


  private def hangup() = {
    if(cur_query != null)
      println("FIXPAUL: abort query")

    Fyrehose.log("connection closed")
    
    socket.close()
  }


  private def finish_query() = {
    Fyrehose.backbone ! QueryExitSig(cur_query)
    cur_query = null
    
    // if (resp.keepalive unary_!)
     reactor ! HangupSig // FIXPAUL: implement keepalive  
  }


  private def stream_query(resp: QueryResponseChunk) : Unit = {
    if (resp.chunk != null)
      write(resp.chunk)
  }


  private def error(msg: String) = {
    Fyrehose.error("endpoint closed: " + msg)
    write(("{\"error\": \""+msg+"\"}").getBytes)
    reactor ! HangupSig
  }


}