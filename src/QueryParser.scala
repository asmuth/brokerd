package com.paulasmuth.fyrehose

class QueryParser {

  var query  : Query = null
  val lexer  = new QueryLexer(this)

  def parse(bdy: QueryBody) = {

    for (pos <- new Range(0, bdy.raw.size - 1, 1))
      lexer.next(bdy.raw(pos).toChar)

    lexer.finish

    if (query == null)
      throw new ParseException("query must contain one of stream, info, etc.")

    query
  }


  def next(token: FQL_TOKEN) : Unit = token match {

    case t: FQL_STREAM  =>
      if (query == null)
        query = eval_query(t)
      else
        throw new ParseException("query can only contain one of stream, info, etc.")

    case t: FQL_TOKEN =>
      if (query == null)
        throw new ParseException("invalid query: must start with stream, info, etc.")
      else
        eval_token(t)

  }


  private def eval_query(token: FQL_TOKEN) : Query = token match {

    case t: FQL_STREAM =>
      new StreamQuery()

  }


  private def eval_token(token: FQL_TOKEN) = token match {

    case t: FQL_OR =>
      query.fstack = query.fstack.or()

    case t: FQL_AND =>
      query.fstack = query.fstack.and()

    case t: FQL_WHERE => t.left match {

      case k: FQL_KEY =>
        query.fstack.push(k)(eval_where(k, t))

      case _ =>
        unexpected_token(t.left.asInstanceOf[FQL_TOKEN],
          "left hand operator of a where clause to be a FQL_KEY")

    }

    case _ => try { query.eval(token) } catch {

      case e: ParseException =>
        unexpected_token(token, e.toString)

    }

  }


  private def eval_where(key: FQL_KEY, token: FQL_WHERE) = token.op match {

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
