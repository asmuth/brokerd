package com.paulasmuth.fyrehose

import scala.actors._
import scala.actors.Actor._
import java.net._
import java.io._
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.CharBuffer

object HangupSig{}

class Endpoint(multiplex: Multiplex, channel: SocketChannel) extends Actor{

  var safe_mode = true  // FIXPAUL: this should be disable-able for performance
  var keepalive = false // FIXPAUL: this should be disable-able for performance

  var cur_query : Query = null

  var parser = new StreamParser(this)
  parser.set_safe_mode(safe_mode)


  def act() = { 
    Actor.loop{ react{ 
      case HangupSig => { exit() }
      case buf: Array[Byte] => read(buf)
      case res: QueryResponseChunk => stream_query(res)
      case qry: QueryBody => exec_query(qry)
      case evt: EventBody => Fyrehose.backbone ! evt
      case ext: QueryExitSig => finish_query()
      // case TIMEOUT => if (cur_query == null) this ! HangupSig // FIXPAUL implement w/o reactWithin
    }}
  }

  
  private def read(buf: Array[Byte]) : Unit = try{
    parser.stream(buf)
  } catch {
    case e: ParseException => error(e.toString)
  }


  private def write(buf: Array[Byte]) = 
    multiplex.push(channel, buf)


  private def hangup() = {
    channel.close(); exit()
  }


  private def exec_query(qry: QueryBody) = try{
    cur_query = QueryParser.parse(qry)
    cur_query ! QueryExecuteSig(this)
    Fyrehose.backbone ! cur_query
  } catch {
    case e: ParseException => error(e.toString)
  }


  private def finish_query() = {
    Fyrehose.backbone ! QueryExitSig(cur_query)
    cur_query = null
    
    // if (resp.keepalive unary_!)
      multiplex.hangup(channel) // FIXPAUL: implement keepalive  
  }


  private def stream_query(resp: QueryResponseChunk) = {
    if (resp.chunk != null)
      write(resp.chunk)
  }


  private def error(msg: String) = {
    Fyrehose.error("endpoint closed: " + msg)
    write(("{\"error\": \""+msg+"\"}").getBytes)
    multiplex.hangup(channel)
  }


}