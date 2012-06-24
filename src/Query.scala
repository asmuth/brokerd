package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

case class QueryBody(raw: Array[Byte])
case class QueryResponseChunk(chunk: Array[Byte])
case class QueryExecuteSig(endpoint: Actor)
case class QueryExitSig(query: Query)
case class QueryDiscoverSig(query: Query, seq_range: (Int, Int))
case class QueryEOFSig()

trait Query extends Actor{

  var recv : Actor = null
  var sequence : Int = 0
  var fstack : FilterStack = new AndFilterStack
  var since : FQL_TVALUE = new FQL_TNOW
  var until : FQL_TVALUE = new FQL_TSTREAM

  def act() = { 
    Actor.loop{ react{
      case QueryExecuteSig(endpoint) => execute(endpoint)
      case QueryEOFSig() => eof()
      case HangupSig => { Fyrehose.log("query finished"); exit() }
      case msg: Message => data(msg)
    }}
  }


  def ready() = since match {

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


  def execute(endpoint: Actor)

  def data(msg: Message)

  def eval(part: FQL_TOKEN)

}
