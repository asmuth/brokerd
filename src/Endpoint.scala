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

  var parser = new StreamParser(self)
  parser.set_safe_mode(safe_mode)


  def act() = { 
    Actor.loop{ react{
      case HangupSig => { println("FIXPAUL: endpoint hangup"); hangup() }
      case buf: Array[Byte] => read(buf)
      case res: QueryResponseChunk => stream_query(res)
      case qry: QueryBody => exec_query(qry)
    }}
  }

  
  private def read(buf: Array[Byte]) : Unit = try{
    parser.stream(buf)
  } catch {
    case e: ParseException => error(e.toString)
  }


  private def write(buf: Array[Byte]) = 
    multixplex.push(channel, buf)


  private def hangup() = {
    channel.close(); exit()
  }


  private def exec_query(qry: QueryBody) = try{
    cur_query = QueryParser.parse(qry)
    cur_query.execute(self)
  } catch {
    case e: ParseException => error(e.toString)
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