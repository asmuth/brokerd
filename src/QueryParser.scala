package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex

object QueryParser extends FQL {

  def parse(bdy: QueryBody) : Query = {
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
