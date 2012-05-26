package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

case class QueryBody(raw: Array[Byte])
case class QueryResponseChunk(chunk: Array[Byte], keepalive: Boolean)
case class QueryExecuteSig(endpoint: Actor)
case class QueryExitSig(query: Query)

trait Query extends Actor{
  
  def act() = { 
    Actor.loop{ react{
      case QueryExecuteSig(endpoint) => execute(endpoint)
      case HangupSig => exit()
    }}
  }

  def execute(endpoint: Actor)

}