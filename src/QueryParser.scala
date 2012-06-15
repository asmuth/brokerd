package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex
import scala.collection.mutable.ListBuffer

class QueryParser {

  var buffer : String = ""
  var cursor : Char = 0
  var query  : Query = null
  val stack  = ListBuffer[FQL_TOKEN]()

  def parse(bdy: QueryBody) = {

    new FQL_ATOM +=: stack

    for (pos <- new Range(0, bdy.raw.size - 1, 1)) {
      cursor  = bdy.raw(pos).toChar

      val next = stack.head.next(cursor, buffer)
      println( stack.head.getClass.getName + " - " + buffer + " - " + cursor )

      if (next != stack.head)
        stack.prepend(next)

      if (stack.head.ready)
        emit(stack.remove(0)) // fixpaul unroll stack

      if ((cursor != ' ') || (buffer.length > 0))
        buffer  = stack.head.buffer(cursor, buffer)

    }

    if (query == null)
      throw new ParseException("query must contain one of stream, info, etc.")

    query
  }


  def emit(token: FQL_TOKEN) : Unit = {
    println("emit: " + token.getClass.getName)
    token match {

    case t: FQL_STREAM  =>
      if (query == null)
        query = build_query(t)
      else
        throw new ParseException("query can only contain one of stream, info, etc.")

    case t: FQL_TOKEN =>
      if (query == null)
        throw new ParseException("invalid query: must start with stream, info, etc.")
      else
        query.eval(t)

  } }


  def build_query(token: FQL_TOKEN) : Query = token match {

    case t: FQL_STREAM =>
      new StreamQuery()

  }

}
