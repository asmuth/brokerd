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

  def act() = { 
    Actor.loop{ react{
      case QueryExecuteSig(endpoint) => execute(endpoint)
      case HangupSig => { Fyrehose.log("query finished"); exit() }
      case msg: Message => data(msg)
    }}
  }

  def execute(endpoint: Actor)

  def data(msg: Message)

  def eval(part: FQL_TOKEN)

}
