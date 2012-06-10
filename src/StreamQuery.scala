package com.paulasmuth.fyrehose

import scala.actors._

class StreamQuery(raw: String) extends Query{

  var recv : Actor = null

  def execute(endpoint: Actor) = 
    recv = endpoint

  def data(event: Event) =
    if (recv == null){
      println("reschedule query event")
      this ! event
    } else {
      println("query outbound stream sent")
      recv ! new QueryResponseChunk(event.bytes)
      recv ! new QueryResponseChunk("\n".getBytes)
    }

}
