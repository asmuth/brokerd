package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

case class QueryBody(raw: Array[Byte])
case class QueryResponseChunk(chunk: Array[Byte])
case class QueryExecuteSig(endpoint: Actor)
case class QueryExitSig(query: Query)

trait Query extends Actor{

  var sequence : Int = 0
  
  def act() = { 
    Actor.loop{ react{
      case QueryExecuteSig(endpoint) => execute(endpoint)
      case HangupSig => { Fyrehose.log("query finished"); exit() }
      case event: Event => data(event)
    }}
  }

  def execute(endpoint: Actor)

  def data(event: Event)

}