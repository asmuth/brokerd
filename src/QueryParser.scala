package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex
import scala.collection.mutable.ListBuffer

trait FQL_TOKEN {
  def buffer(cur: Char, buf: String) : String
  def ready : Boolean
  def next(cur: Char, buf: String) : FQL_TOKEN
}

trait FQL_MTOKEN extends FQL_TOKEN {
  var cur : Char = 0
  var buf : String = null
  def buffer(cur: Char, buf: String) : String =
    if (ready) "" else buf + cur
  def next(_cur: Char, _buf: String) : FQL_TOKEN =
    { cur=_cur; buf = _buf; next }
  def next : FQL_TOKEN
}

class FQL_ATOM extends FQL_MTOKEN {
  def ready =
    (cur == ' ') || (cur == '(')
  def next =
    if ((cur != ' ') && (cur != '('))
      this
    else buf match {
      case "stream"    => new FQL_STREAM
      case "where"     => new FQL_WHERE(true)
      case "where_not" => new FQL_WHERE(false)
    }
}

class FQL_STREAM extends FQL_MTOKEN {
  def ready = true
  def next = this
}

class FQL_WHERE(negated: Boolean) extends FQL_MTOKEN {
  var key : String = null
  var value : String = null

  def ready = false

  def next =
    if (key == null){ key = "fooabr"; this }
    else this
}

class QueryParser extends FQL {

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
