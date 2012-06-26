package com.paulasmuth.fyrehose

import scala.actors._

class CountQuery() extends Query{

  var count : Int = 0

  def data(msg: Message) =
    if (matches(msg))
      count += 1

  def eval(token: FQL_TOKEN) =
    throw new ParseException("expected nothing")

  override def finish = {
    recv ! QueryResponseChunk(("{ \"count\": " + count.toString + " }\n").getBytes)
    recv ! QueryExitSig(this)
  }

  override def assert = until match {
    case t: FQL_TSTREAM =>
      throw new ParseException("can't COUNT over unbound time range (UNTIL STREAM)")
    case _ => ()
  }

}
