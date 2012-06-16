package com.paulasmuth.fyrehose

import scala.actors._

class StreamQuery() extends Query{

  var recv : Actor = null

  def execute(endpoint: Actor) =
    recv = endpoint


  def data(event: Event) =
    if (recv == null){
      this ! event
    } else if(fstack.eval(event)) {
      // FIXPAUL: race condition ahead!
      recv ! new QueryResponseChunk(event.bytes)
      recv ! new QueryResponseChunk("\n".getBytes)
    }


  def eval(token: FQL_TOKEN) =
     throw new ParseException("expected nothing")


}
