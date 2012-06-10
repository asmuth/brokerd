package com.paulasmuth.fyrehose

import scala.actors._

class StreamQuery(raw: String) extends Query{

  val X_VALIDATE = """^(([a-z]+\([^\)]*\)|and|or) *)+$"""
  val X_EXTRACT  = """(([a-z]+)\(([^\)]*)\)|and|or)"""

  var recv : Actor = null
  var filters = List[String]()


  if (raw.matches(X_VALIDATE) unary_!)
    throw new ParseException("invalid query: " + raw)

  val xparse = java.util.regex.Pattern
    .compile(X_EXTRACT)
    .matcher(raw)

  while(xparse.find())
    eval(raw.substring(xparse.start, xparse.end))


  def execute(endpoint: Actor) =
    recv = endpoint


  def data(event: Event) =
    if (recv == null){
      println("reschedule query event")
      this ! event
    } else {
      println("query outbound stream sent")
      recv ! new QueryResponseChunk(event.bytes)
      recv ! new QueryResponseChunk("\n".getBytes)
    }


  private def eval(part: String) = {
    println("parsing: " + part)
  }


}
