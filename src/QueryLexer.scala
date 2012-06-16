package com.paulasmuth.fyrehose

import scala.collection.mutable.ListBuffer

class QueryLexer(recv: QueryParser) {

  var buffer : String = ""
  val stack  = ListBuffer[FQL_TOKEN]()

  new FQL_ATOM +=: stack

  def next(cursor: Char) : Unit = {

    println( stack.head.getClass.getName + " - " + buffer + " - " + cursor )

    if ((cursor == ' ') && (buffer.size == 0))
      return ()

    val next = stack.head.next(cursor, buffer)

    if (next != stack.head)
      next +=: stack

    if (stack.head.ready)
      ready

    buffer  = stack.head.buffer(cursor, buffer)

  }


  def finish : Unit = {
    next(' ')

    if (stack.size > 1)
      throw new ParseException("unfinished statement")
  }


  private def emit : Unit =
    recv.emit(stack.remove(0))


  private def next_ready : Boolean =
    (stack.size > 1) && (stack.head.ready)


  private def ready : Unit =
    while (next_ready) stack(1) match {

      case a: FQL_ATOM =>
        emit

      case s: FQL_STATEMENT =>
        statement(s.next(stack.head))

    }


  private def statement(next: FQL_TOKEN) = {
    stack.remove(0)

    if (next != stack.head)
      next +=: stack

  }


}

