package com.paulasmuth.fyrehose

import scala.actors._

// fixpaul: since(), until(), less/greater-than, regex, include exists
class StreamQuery(raw: String) extends Query{

  val X_VALIDATE = """^(([a-z_]+\([^\)]*\)|and|or) *)+$"""
  val X_EXTRACT  = """(([a-z_]+)\(([^\)]*)\)|and|or)"""

  val X_STREAM = """^stream\(\)$""".r
  val X_OR     = """^or$""".r
  val X_AND    = """^and$""".r

  trait X_WHERE {

    def regex : String
    def eval(v: String) : String => Boolean

    def unapply(s: String) : Option[(String, (String => Boolean))] = {
      val x_where_true = ("""^where\(([^ ]+)""" + regex).r
      val x_where_false = ("""^where_not\(([^ ]+)""" + regex).r
      s match {
        case x_where_true(k: String, v: String) =>
          return Some(((k, eval(v))))
        case x_where_false(k: String, v: String) =>
          return Some(((k, negated(eval(v)))))
        case _ => 
          return None
      }
    }

    def negated(l: String => Boolean): String => Boolean =
      (x: String) => l(x) unary_!

  }

  object X_EQUALS_STR extends X_WHERE {
    def regex = """ *= *'([^']*)'\)$"""
    def eval(v: String) = (x: String) => x == v
  }

  object X_EQUALS_INT extends X_WHERE {
    def regex = """ *= *([0-9]+)\)$"""
    def eval(v: String) = (x: String) => x.toInt == v.toInt
  }

  object X_EQUALS_DBL extends X_WHERE {
    def regex = """ *= *([0-9]+\.[0-9]+)\)$"""
    def eval(v: String) = (x: String) => x.toDouble == v.toDouble
  }


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
      this ! event
    } else if(fstack.eval(event)) {
      // FIXPAUL: race condition ahead!
      recv ! new QueryResponseChunk(event.bytes)
      recv ! new QueryResponseChunk("\n".getBytes)
    }


  private def parse(part: String) = {
    part match {

      case X_STREAM() =>
        ()

      case X_OR() =>
        fstack = fstack.or()

      case X_AND() =>
        fstack = fstack.and()

      case X_EQUALS_STR(k: String, l: (String => Boolean)) =>
        fstack.push(k)(l)

      case X_EQUALS_INT(k: String, l: (String => Boolean)) =>
        fstack.push(k)(l)

      case X_EQUALS_DBL(k: String, l: (String => Boolean)) =>
        fstack.push(k)(l)

      case _ =>
        throw new ParseException("invalid query part: " + part)

    }

  }


}
