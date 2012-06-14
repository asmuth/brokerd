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

  def eval(token: X_TOKEN) = () /*= match {

        case part: String =>
          throw new ParseException("invalid query part: " + part)
    //case X_STREAM() =>
    // ()

    //case X_OR() =>
    //  fstack = fstack.or()

    //case X_AND() =>
    //  fstack = fstack.and()

    case 'equals_str => ()
      //(fstack.push _).tupled(token.udata)

    //case X_EQUALS_INT(k: String, l: (String => Boolean)) =>
    // fstack.push(k)(l)

    //case X_EQUALS_DBL(k: String, l: (String => Boolean)) =>
    //  fstack.push(k)(l)

    case _ =>
      throw new ParseException("invalid query token: " + token.symbol.toString)

  }*/


}
