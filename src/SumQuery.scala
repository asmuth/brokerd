package com.paulasmuth.fyrehose

import scala.actors._

class SumQuery() extends Query{

  def data(msg: Message) = ()

  def eval(token: FQL_TOKEN) =
    throw new ParseException("invalid query token: " + token.getClass.getName)

}


