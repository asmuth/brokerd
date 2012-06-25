package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

case class QueryBody(raw: Array[Byte])
case class QueryResponseChunk(chunk: Array[Byte])
case class QueryExecuteSig(endpoint: Actor)
case class QueryExitSig(query: Query)
case class QueryDiscoverSig(query: Query, seq_range: (Int, Int))
case class QueryEOFSig()
case class QueryReadySig()

trait Query extends Actor{

  var recv : Actor = null
  var sequence : Int = 0
  var fstack : FilterStack = new AndFilterStack
  var since : FQL_TVALUE = new FQL_TNOW
  var until : FQL_TVALUE = new FQL_TSTREAM
  val now = FyrehoseUtil.now

  def act() = { 
    Actor.loop{ react{
      case QueryExecuteSig(endpoint) => execute(endpoint)
      case QueryEOFSig() => eof()
      case QueryReadySig => ready()
      case HangupSig => exit()
      case msg: Message => data(msg)
    }}
  }


  private def ready() = since match {

    case tsince: FQL_TNOW => ()

    case tsince: FQL_TUNIX => until match {

      case tuntil: FQL_TNOW =>
        Fyrehose.message_cache ! QueryDiscoverSig(this,
          Fyrehose.message_index.seq_range(tsince, sequence))

      case tuntil: FQL_TSTREAM =>
        Fyrehose.message_cache ! QueryDiscoverSig(this,
          Fyrehose.message_index.seq_range(tsince, sequence))

      case tuntil: FQL_TUNIX =>
        Fyrehose.message_cache ! QueryDiscoverSig(this,
          Fyrehose.message_index.seq_range(tsince, tuntil))

    }

  }


  def eof() = until match {
    case tuntil: FQL_TSTREAM => ()
    case _ => recv ! QueryExitSig(this)
  }


  def matches(msg: Message) =
    since_matches(msg) &&
    until_matches(msg) &&
    fstack.eval(msg)


  def since_matches(msg: Message) = since match {
    case t: FQL_TNOW   => msg.time >= now
    case t: FQL_TUNIX => msg.time >= t.get
  }


  def until_matches(msg: Message) = until match {
    case t: FQL_TNOW   => msg.time <= now
    case t: FQL_TUNIX => msg.time <= t.get
    case _ => true
  }


  def execute(endpoint: Actor)

  def data(msg: Message)

  def eval(part: FQL_TOKEN)


  /*override def exceptionHandler = {
    case e: Exception => Fyrehose.error("query exploded: " + e.toString)
  }*/

}
