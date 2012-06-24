package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.ListBuffer

class MessageCache extends Actor {

  var cache_size = 10

  val messages = ListBuffer[Message]()
  var sequence = 0

  def act = loop { react {
    case m: Message => next(m)
  }}


  def next(msg: Message) =
    if (msg.sequence != sequence + 1)
      this ! msg
    else
      push(msg)


  def trim =
    messages.trimStart(messages.size - cache_size + 1)


  def push(msg: Message) = {
    messages += msg
    sequence += 1

    println("mem cache: " + messages.size + " | " + messages.first.sequence.toString + "-" + messages.last.sequence.toString)

    if (messages.size > cache_size)
      trim

  }


}
