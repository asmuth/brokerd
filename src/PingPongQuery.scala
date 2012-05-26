package com.paulasmuth.fyrehose

import scala.actors._

class PingPongQuery(raw: Array[Byte]) extends Query{

  def execute(endpoint: Actor){
    endpoint ! new QueryResponseChunk((new String(raw)).getBytes, false)
    endpoint ! QueryExitSig(this)
  }
    
}