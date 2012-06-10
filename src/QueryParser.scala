package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex

object QueryParser{

  def parse(bdy: QueryBody) : Query = {

    val qry_str = new String(bdy.raw)
    var query: Query = null

    if (qry_str.matches(""".*stream\(.*"""))
      query = new StreamQuery(qry_str)

    else if (qry_str.matches(""".*info\(.*"""))
      query = new InfoQuery(qry_str)

    else
      throw new ParseException("invalid query: " + qry_str)

    return query
  }

}
