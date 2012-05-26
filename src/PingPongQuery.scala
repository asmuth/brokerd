package com.paulasmuth.fyrehose

import scala.actors._

class PingPongQuery(str: String) extends Query{

  def execute(endpoint: Actor){
    endpoint ! QueryResponseChunk(str.getBytes)
    endpoint ! QueryExitSig(this)
  }

  def data(event: Event){}
    
}