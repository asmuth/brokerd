package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

class QueryResponseChunk(_raw: Array[Byte], _ka: Boolean){
  def chunk = _raw
  def keepalive = _ka
}

class Query(raw: Array[Byte]) extends Actor{

  case class Execute(endpoint: Actor)
  
  def act() = { 
    Actor.loop{ react{
      case Execute(endpoint) => execute_async(endpoint)
    }}
  }


  def execute(endpoint: Actor) = {
    println("EXEC")
    self ! Execute(endpoint)    
  }


  private def execute_async(endpoint: Actor) = {
    println("EXEC ASYNC")
    endpoint ! new QueryResponseChunk(("you said: " + new String(raw)).getBytes, false)
  }


}