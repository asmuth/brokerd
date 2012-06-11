package com.paulasmuth.fyrehose

import scala.actors._

class StreamQuery(raw: String) extends Query{

  val X_VALIDATE = """^(([a-z]+\([^\)]*\)|and|or) *)+$"""
  val X_EXTRACT  = """(([a-z]+)\(([^\)]*)\)|and|or)"""

  val x_stream = """^stream\(\)$""".r
  val x_or     = """^or$""".r
  val x_and    = """^and$""".r
  val x_where  = """^where\(([^ ]+)"""

  val x_where_equals_str = (x_where + """ *= *'([^']*)'\)$""").r
  val x_where_equals_int = (x_where + """ *= *([0-9]+)\)$""").r
  val x_where_equals_dbl = (x_where + """ *= *([0-9]+\.[0-9]+)\)$""").r


  var recv : Actor = null
  var fstack : FilterStack = new AndFilterStack


  if (raw.matches(X_VALIDATE) unary_!)
    throw new ParseException("invalid query: " + raw)

  val xparse = java.util.regex.Pattern
    .compile(X_EXTRACT)
    .matcher(raw)

  while(xparse.find())
    parse(raw.substring(xparse.start, xparse.end))


  def execute(endpoint: Actor) =
    recv = endpoint


  def data(event: Event) =
    if (recv == null){
      println("reschedule query event")
      this ! event
    } else if(fstack.eval(event)) {
      println("query outbound stream sent")
      recv ! new QueryResponseChunk(event.bytes)
      recv ! new QueryResponseChunk("\n".getBytes)
    }


  // fixpaul: where_not, since(), until(), less/greater-than, regex, include exists
  private def parse(part: String) = {
    println("parsing: " + part)

    part match {

      case x_stream() =>
        ()

      case x_or() =>
        fstack = fstack.or()

      case x_and() =>
        fstack = fstack.and()

      case x_where_equals_str(k: String, v: String) =>
        fstack.push(k)((x: String) => x == v)

      case x_where_equals_int(k: String, v: String) =>
        fstack.push(k)((x: String) => x.toInt == v.toInt)

      case x_where_equals_dbl(k: String, v: String) =>
        fstack.push(k)((x: String) => x.toDouble == v.toDouble)

      case _ =>
        throw new ParseException("invalid query part: " + part)

    }

  }


}
