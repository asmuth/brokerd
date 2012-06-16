package com.paulasmuth.fyrehose

import scala.actors._

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
        fstack.push(k)(eval_filter(k, t))

      case _ =>
        throw new ParseException("left hand operator of a where clause must be a FQL_KEY")

    }

    case _ =>
      throw new ParseException("invalid token: " + token.getClass.getName)

  }


  def eval_filter(key: FQL_KEY, token: FQL_WHERE) = token.op match {

    case o: FQL_OPERATOR_EQUALS => token.right match {

      case v: FQL_STRING =>
        (m: Event) => m.getAsString(key) == v.get

      case v: FQL_KEY =>
        (m: Event) => m.getAsString(key) == m.getAsString(v)

      case v: FQL_INTEGER =>
        (m: Event) => { println("get: " + key.get + " -> " +  m.getAsInteger(key).toString + " vs " + v.get.toString); m.getAsInteger(key) == v.get }

      case v: FQL_FLOAT =>
        (m: Event) => m.getAsDouble(key) == v.get

    }

    case _ =>
      throw new ParseException("invalid token: " + token.getClass.getName)

  }


}
