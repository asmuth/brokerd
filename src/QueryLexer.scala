package com.paulasmuth.fyrehose

import scala.collection.mutable.ListBuffer

class QueryLexer(recv: QueryParser) {

  var buffer : String = ""
  var cursor : Char   = 0
  val stack  = ListBuffer[FQL_TOKEN]()
  val args   = ListBuffer[FQL_TOKEN]()

  new FQL_ATOM +=: stack

  def next(cur: Char) : Unit = 
    { cursor = cur; next }

  def next : Unit = {

    val head = stack.head.next(cursor, buffer)
    buffer   = stack.head.buffer(cursor, buffer)

    debug

    if (head != stack.head)
      { head +=: stack; next }

    if ((stack.size < 2) || (stack.head.ready unary_!))
      return ()

    stack(1) match {

      case a: FQL_ATOM =>
        recv.emit(stack.remove(0))

      case m: FQL_META =>
        { println("REPLACE"); stack.remove(1) }

      case s: FQL_STATEMENT =>
        (stack.remove(0) :: args.toList).foreach
          { arg => statement(s, arg); args -= arg }

      case t: FQL_TOKEN =>
        { println("ARGSTACK"); stack.remove(0) +=: args }

    }

    next
  }

  def finish : Unit = {
    next(' ')

    if (stack.size > 1)
      throw new ParseException("invalid query, expected " +
        stack.head.getClass.getName.replaceFirst(""".*\.""", "") + 
        " but found: >>" + buffer + "<<")
  }


  private def statement(statement: FQL_STATEMENT, arg: FQL_TOKEN) = {

    val head = statement.next(arg)
    println("statement: " + arg.getClass.getName + " => " + head.getClass.getName)

    if (head != stack.head)
      head +=: stack

  }


  private def debug =
    println(
      " " + (if (stack.head.ready) "X" else " ") +
      " | " + cursor + " | " + buffer +
      (" " * (15 - buffer.size)) + " | " +
      (stack.toString.substring(5).replaceAll("""[a-z@\.\(\)0-9]""", "")) + " (" +
      (args.toString.substring(5).replaceAll("""[a-z@\.\(\)0-9]""", "")) + ")")
}

