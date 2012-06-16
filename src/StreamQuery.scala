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


  def eval(token: FQL_TOKEN) = token match {

    case t: FQL_OR =>
      fstack = fstack.or()

    case t: FQL_AND =>
      fstack = fstack.and()

    case t: FQL_WHERE => t.left match {

      case k: FQL_KEY =>
        fstack.push(k)(eval_filter(t))

      case _ =>
        throw new ParseException("left hand operator of a where clause must be a FQL_KEY")

    }

    case _ =>
      throw new ParseException("invalid token: " + token.getClass.getName)

  }


  def eval_filter(token: FQL_WHERE) = token.op match {

    case o: FQL_OPERATOR_EQUALS => token.right match {

      case v: FQL_STRING =>
        (x: String) => v.get == x

    }

    case _ =>
      throw new ParseException("invalid token: " + token.getClass.getName)

  }


}
