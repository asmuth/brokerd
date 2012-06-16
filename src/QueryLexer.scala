package com.paulasmuth.fyrehose

import scala.collection.mutable.ListBuffer

class QueryLexer(recv: QueryParser) {

  var buffer : String = ""
  val stack  = ListBuffer[FQL_TOKEN]()

  new FQL_ATOM +=: stack

  def next(cursor: Char) : Unit = {

    val next = stack.head.next(cursor, buffer)
    println( stack.head.getClass.getName + " - " + buffer + " - " + cursor )

    if (next != stack.head)
      next +=: stack

    if (stack.head.ready)
      ready

    if ((cursor != ' ') || (buffer.length > 0))
      buffer  = stack.head.buffer(cursor, buffer)

  }


  def pop : Unit =
    recv.emit(stack.remove(0))


  def next_ready : Boolean =
    (stack.size > 1) && (stack.head.ready)


  def ready : Unit = 
    while (next_ready) stack(1) match {

      case a: FQL_ATOM => 
        pop

      case s: FQL_STATEMENT =>
        statement(s.next(stack.head))

    }


  def statement(next: FQL_TOKEN) =
    if (next != stack.head)
      { pop; next +=: stack } 
    else
      pop

}

