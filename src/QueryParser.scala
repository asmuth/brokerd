package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex


trait FQL_TOKEN {
  val p_whitespace = true
  val p_braces = true
  def ready : Boolean
  def next(t: String) : FQL_TOKEN
}

class FQL_ATOM extends FQL_TOKEN {
  def ready = true
  def next(t: String) = t match {

    case "stream"    => new FQL_STREAM
    case "where"     => new FQL_WHERE(true)
    case "where_not" => new FQL_WHERE(false)

  }
}

class FQL_STREAM extends FQL_TOKEN {
  def ready = true
  def next(t: String) = this
}

class FQL_WHERE(negated: Boolean) extends FQL_TOKEN {
  var key : String = null
  var value : String = null
  var value : String = null

  def p_braces =
    (key != null) && 

  def ready = false

  def next(t: String) =
    if (key == null){ key = t; this }
    else this
}

object QueryParser extends FQL {

  def parse(bdy: QueryBody) = {
    var buf_str = ""
    var cur : FQL_TOKEN = new FQL_ATOM

    def next = {
      println("next: " + buf_str)
      cur = cur.next(buf_str)
      buf_str = ""

      if (cur.ready) {
        println("PPPPARSED: " + cur.getClass.getName)
        cur = new FQL_ATOM
      }
    }

    for (pos <- new Range(0, bdy.raw.size - 1, 1)) {

      if ((bdy.raw(pos) == ' ') && (cur.p_whitespace))
        next

      else if (
        ((bdy.raw(pos) == '(') || (bdy.raw(pos) == '(')) &&
        ((pos == 0) || (bdy.raw(pos - 1) != '\\')) &&
        cur.p_braces)
        next


      else
        buf_str = buf_str + bdy.raw(pos).toChar


      println( cur.getClass.getName + " - " + buf_str )

    }

    new StreamQuery()
  }


  def parsed(bdy: QueryBody) : Query = {
    val raw = new String(bdy.raw)
    var query: Query = null

    if (raw.matches(X_VALIDATE) unary_!)
      throw new ParseException("invalid query: " + raw)

    val xparse = java.util.regex.Pattern
      .compile(X_EXTRACT)
      .matcher(raw)

    while(xparse.find())
      raw.substring(xparse.start, xparse.end) match {

        case X_COMMAND(t: X_TOKEN) =>
          if (query == null)
            query = t.value.asInstanceOf[Class[_ <: Any]]
              .newInstance.asInstanceOf[Query]
          else
            throw new ParseException("query can only contain one of stream, info, etc")

        case X_KEYWORD(t: X_TOKEN) =>
          if (query == null) 
            throw new ParseException("invalid query: must start with stream, info, etc.")
          else
            query.eval(t)

        case X_WHERE(t: X_TOKEN) =>
          if (query == null)
            throw new ParseException("invalid query: must start with stream, info, etc.")
          else
            query.eval(t)

        case part: String =>
          throw new ParseException("invalid query token: " + part)

      }

    return query
  }

}
