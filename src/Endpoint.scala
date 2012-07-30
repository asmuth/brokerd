package com.paulasmuth.fyrehose

import scala.actors._
import scala.actors.Actor._
import java.io._
import java.net._

object HangupSig{}
object TimeoutSig{}

class Endpoint(socket: Socket) extends Runnable with Receivable {

  var safe_mode = true  // FIXPAUL: this should be disable-able for performance
  var keepalive = false // FIXPAUL: this should be disable-able for performance

  var cur_query : Query = null
  var idle_status       = true
  var async_kill        = false

  socket.setSoTimeout(Fyrehose.CONN_IDLE_TIMEOUT)
  val in_stream  = socket.getInputStream()
  val out_stream = socket.getOutputStream()

  val stream = new InboundStream(this, Fyrehose.BUFFER_SIZE_PARSER)
  stream.set_safe_mode(safe_mode)

  Fyrehose.log("connection opened")
  Fyrehose.tcp_listener.num_connections.incrementAndGet

  val reactor = new Actor {
    def qsize = mailboxSize
    def act = loop { receive{ 
      case HangupSig  => { hangup(); exit() }
      case TimeoutSig => timeout()
      case resp: QueryResponseChunk => write(resp.chunk)
      case xsig: QueryExitSig => query_finished()
      case data: Array[Byte] => recv(data)
    }
  }}

  reactor.start()

  def run = {
    var buffer = new Array[Byte](Fyrehose.BUFFER_SIZE_TCP)
    var next   = 0

    try{
      while (((next > -1) || (idle_status unary_!)) && (async_kill unary_!)){
        if (next > 0)
          reactor ! java.util.Arrays.copyOfRange(buffer, 0, next)

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


  def recv(data: Array[Byte]) : Unit =
    if (async_kill) return ()
    else if (reactor.qsize > Fyrehose.CONN_MAX_QUEUE_SIZE)
      error("queue too big: " + reactor.qsize.toString, true)
    else try {
      stream.read(data, data.size)
    } catch {
      case e: ParseException => error(e.toString, true)
    }


  def message(ev_body: MessageBody) = try{
    Fyrehose.backbone ! new Message(ev_body.raw)
  } catch {
    case e: ParseException => error(e.toString, true)
  }


  def query(qry: QueryBody) = try{
    cur_query = (new QueryParser).parse(qry)

    if (cur_query == null)
      throw new ParseException("invalid query")

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
    Fyrehose.tcp_listener.num_connections.decrementAndGet
    socket.close()
  }


  private def timeout() : Unit = {
    if(cur_query != null)
      return ()

    error("read timeout", true)
  }


  private def error(msg: String, recoverable: Boolean) : Unit = {
    if(async_kill)
      return ()

    Fyrehose.error("endpoint closed: " + msg)

    if (recoverable)
      write(("{\"error\": \""+msg+"\"}\n").getBytes) // FIXPAUL

    close_connection()
  }


  private def close_connection() = {
    async_kill = true
    query_abort()
    reactor ! HangupSig
  }

}
