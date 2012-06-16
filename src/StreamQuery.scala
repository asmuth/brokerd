package com.paulasmuth.fyrehose

import scala.actors._

class StreamQuery() extends Query{

  var recv : Actor = null

  def execute(endpoint: Actor) =
    recv = endpoint


  def data(msg: Message) =
    if (recv == null){
      this ! msg
    } else if(fstack.eval(msg)) {
      // FIXPAUL: race condition ahead!
      recv ! new QueryResponseChunk(msg.bytes)
      recv ! new QueryResponseChunk("\n".getBytes)
    }


  def eval(token: FQL_TOKEN) =
     throw new ParseException("expected nothing")


}
