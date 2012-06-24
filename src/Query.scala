package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

case class QueryBody(raw: Array[Byte])
case class QueryResponseChunk(chunk: Array[Byte])
case class QueryExecuteSig(endpoint: Actor)
case class QueryExitSig(query: Query)

trait Query extends Actor{

  var sequence : Int = 0
  var fstack : FilterStack = new AndFilterStack
  var since : FQL_TVALUE = null
  var until : FQL_TVALUE = null

  def act() = { 
    Actor.loop{ react{
      case QueryExecuteSig(endpoint) => execute(endpoint)
      case HangupSig => { Fyrehose.log("query finished"); exit() }
      case msg: Message => data(msg)
    }}
  }


  def ready() = ()
    /*if(self.since == "now")
      // do nothing

    else if(self.until == "now")
      load_messages(sequence_from_index(self.since), sequence)

    else if(self.until == "stream")
      load_messages(sequence_from_index(self.since), sequence)

    else
      load_messages(sequence_from_index(self.since), sequence_from_index(self.until))*/


  def execute(endpoint: Actor)

  def data(msg: Message)

  def eval(part: FQL_TOKEN)

}
