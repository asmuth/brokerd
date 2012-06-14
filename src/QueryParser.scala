package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex

object QueryParser extends FQL{

  X_KEYWORD.define('and)
  X_KEYWORD.define('or)

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

    /*    case X_QUERY(q: Query) =>
          if (query == null)
            query = q
          else
            throw new ParseException("query can only contain one of stream, info, etc")

*/
        case X_KEYWORD(t: X_TOKEN) =>
          println("TOKEN! " + t.key)

        /*
          if (query == null) 
            throw new ParseException("invalid query: must start with stream, info, etc.")
          else
            query.eval(t)*/

        case part: String =>
          throw new ParseException("invalid query token: " + part)

      }

    return query
  }

}
