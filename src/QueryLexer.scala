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
      emit

    if ((cursor != ' ') || (buffer.length > 0))
      buffer  = stack.head.buffer(cursor, buffer)

  }


  def emit() : Unit = {
    recv.emit(stack.remove(0)) // fixpaul unroll stack
  }


}

