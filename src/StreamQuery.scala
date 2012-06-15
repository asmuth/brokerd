package com.paulasmuth.fyrehose

import scala.actors._

// fixpaul: since(), until(), less/greater-than, regex, include exists
class StreamQuery() extends Query{

  var recv : Actor = null
  var fstack : FilterStack = new AndFilterStack

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

  def eval(part: FQL_TOKEN) = ()
/*
    case 'or =>
      fstack = fstack.or()

    case 'and =>
      fstack = fstack.and()

    case 'where =>
      fstack.push(token.key.name)(eval_filter(token))

    case _ =>
      throw new ParseException("invalid query token: " + token.name.toString)

  }
*/


/*  def eval_filter(token: X_TOKEN) = token.key match {

    case 'equals_str =>
      (x: String) => x == token.value.asInstanceOf[String]

    case _ =>
      throw new ParseException("invalid query token: " + token.name.toString)

  }*/
}
