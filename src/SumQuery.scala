package com.paulasmuth.fyrehose

import scala.actors._

class SumQuery() extends Query {

  var sum_key : FQL_KEY = null
  var sum : Double = 0


  def data(msg: Message) = try {
    if (matches(msg) && msg.exists(sum_key.get))
      sum += msg.getAsDouble(sum_key)
  } catch {
    case e: java.lang.NumberFormatException => ()
  }


  def eval(token: FQL_TOKEN) =
    throw new ParseException("invalid query token: " + token.getClass.getName)


  override def finish = {
    recv ! QueryResponseChunk(("{ \"sum\": " + sum.toString + " }\n").getBytes)
    recv ! QueryExitSig(this)
  }


  override def assert = {

    until match {
      case t: FQL_TSTREAM =>
        throw new ParseException("can't COUNT over unbound time range (UNTIL STREAM)")
      case _ => ()
    }

    cmd.arg1 match {
      case k: FQL_KEY => sum_key = k
      case _ => throw new ParseException("invalid argument for SUM: " + cmd.arg1.getClass.getName)
    }

  }


}


