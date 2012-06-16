package com.paulasmuth.fyrehose

import scala.collection.mutable.ListBuffer

class QueryLexer(recv: QueryParser) {

  var buffer : String = ""
  var cursor : Char   = 0
  val stack  = ListBuffer[FQL_TOKEN]()

  new FQL_ATOM +=: stack

  def next(cur: Char) : Unit = 
    { cursor = cur; next }

  def next : Unit = {

    val head = stack.head.next(cursor, buffer)
    buffer   = stack.head.buffer(cursor, buffer)

    debug

    if (head != stack.head)
      { head +=: stack; next }

    else if (stack.head.ready)
      ready

  }

  def finish : Unit = {
    next(' '); next(' ')

    if (stack.size > 1)
      error
  }


  private def next_ready : Boolean =
    (stack.size > 1) && (stack.head.ready)


  private def ready() : Unit = {
    val args  = ListBuffer[FQL_TOKEN]()

    while (next_ready) stack(1) match {

      case a: FQL_ATOM =>
        recv.emit(stack.remove(0))

      case s: FQL_STATEMENT =>
        if (args.size == 0)
          statement(s.next(stack.head))
        else
          statement(s.next(args.remove(0)))

      case m: FQL_META =>
        { println("REPLACE"); stack.remove(1); next }

      case t: FQL_TOKEN =>
        stack.remove(0) +=: args; next

    }
  }

  private def statement(head: FQL_TOKEN) = {
    stack.remove(0)

    if (head != stack.head)
      head +=: stack

    next
  }


  private def error =
    throw new ParseException("invalid query, expected " +
      stack.head.getClass.getName.replaceFirst(""".*\.""", "") + 
      " but found: >>" + buffer + "<<")


  private def debug =
    println(
      " " + (if (stack.head.ready) "X" else " ") +
      " | " + cursor + " | " + buffer + 
      (" " * (15 - buffer.size)) + " | " + 
      (stack.toString.substring(5).replaceAll("""[a-z@\.\(\)0-9]""", "")) )
}

