package com.paulasmuth.fyrehose

import scala.collection.mutable.ListBuffer

class QueryLexer(recv: QueryParser) {

  var buffer : String = ""
  var cursor : Char   = 0
  val stack  = ListBuffer[FQL_TOKEN]()

  new FQL_ATOM +=: stack

  def next(cur: Char) : Unit =
    //if (trim(cursor) unary_!)
      { cursor = cur; next }

  def next : Unit = {

    println( stack.head.getClass.getName + " - " + buffer + " - " + cursor )

    val cur = stack.head.next(cursor, buffer)
    buffer  = stack.head.buffer(cursor, buffer)

    if (cur != stack.head)
      { cur +=: stack; next }

    else if (stack.head.ready)
      ready

  }

  def finish : Unit = {
    next(' '); next(' ')

    if (stack.size > 1)
      throw new ParseException("unfinished statement")
  }


  private def next_ready : Boolean =
    (stack.size > 1) && (stack.head.ready)


  private def ready : Unit =
    while (next_ready) stack(1) match {

      case a: FQL_ATOM =>
        recv.emit(stack.remove(0))

      case s: FQL_STATEMENT =>
        statement(s.next(stack.head))

    }


  private def statement(next: FQL_TOKEN) = {
    stack.remove(0)

    if (next != stack.head)
      next +=: stack

  }


  private def trim(cursor: Char) : Boolean =
    (cursor == ' ') && 
    (buffer.length > 0) && 
    (buffer.charAt(buffer.size - 1) == ' ')


}

