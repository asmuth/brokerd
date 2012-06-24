package com.paulasmuth.fyrehose

import scala.actors._

class StreamQuery() extends Query{

  def execute(endpoint: Actor) =
    recv = endpoint


  def data(msg: Message) =
    if (matches(msg) unary_!) ()
    else if (recv == null) {
      this ! msg
    } else { // FIXPAUL: race condition ahead!
      recv ! new QueryResponseChunk(msg.bytes)
      recv ! new QueryResponseChunk("\n".getBytes)
    }


  def eval(token: FQL_TOKEN) =
    throw new ParseException("expected nothing")


}
