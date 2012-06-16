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
        unexpected_token(t.left.asInstanceOf[FQL_TOKEN],
          "left hand operator of a where clause to be a FQL_KEY")

    }

    case _ =>
      unexpected_token(token, "FQL_ATOM")

  }


  def eval_filter(key: FQL_KEY, token: FQL_WHERE) = token.op match {

    case o: FQL_OPERATOR_EQUALS => token.right match {

      case v: FQL_STRING =>
        (m: Event) => m.getAsString(key) == v.get

      case v: FQL_KEY =>
        (m: Event) => m.getAsString(key) == m.getAsString(v)

      case v: FQL_INTEGER =>
        (m: Event) => m.getAsInteger(key) == v.get

      case v: FQL_FLOAT =>
        (m: Event) => m.getAsDouble(key) == v.get

      case v: FQL_BOOL =>
        (m: Event) => m.getAsBoolean(key) == v.get

      case _ =>
        unexpected_token(token.right.asInstanceOf[FQL_TOKEN], "FQL_VAL")

    }

    case _ =>
      unexpected_token(token.op.asInstanceOf[FQL_TOKEN], "FQL_OPERATOR")

  }


  private def unexpected_token(found: FQL_TOKEN, expected: String) =
    throw new ParseException("unexpected token: " +  found.getClass.getName
      .replaceAll("[^A-Z_]", "") + ", expected: " + expected)

}
