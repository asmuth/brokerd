package com.paulasmuth.fyrehose

import scala.actors._

class StreamQuery(raw: String) extends Query{

  var recv : Actor = null

  def execute(endpoint: Actor) = 
    recv = endpoint

  def data(event: Event) = 
    if (recv == null)
      this ! event
    else {
      recv ! new QueryResponseChunk(event.bytes)
      recv ! new QueryResponseChunk("\n".getBytes)        
    }
    
}