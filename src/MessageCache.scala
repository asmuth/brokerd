package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.ListBuffer

class MessageCache extends Actor {

  val cache_size = Fyrehose.MESSAGE_CACHE_SIZE

  val messages = ListBuffer[Message]()
  var sequence = 0

  def act = loop { react {
    case msg: Message => next(msg)
    case sig: QueryDiscoverSig => retrieve(sig)
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

  def forward(seq_range: (Int, Int)) =
    println("foward " + seq_range.toString)


  def retrieve(sig: QueryDiscoverSig) : Unit = {
    println("memcache request: " + sig.seq_range._1.toString + " - " + sig.seq_range._2.toString)

    if (sig.seq_range == ((-1, 0)))
      return sig.query ! QueryEOFSig()

    if (sig.seq_range._2 < messages.first.sequence)
      return forward(sig.seq_range)

    var ind = messages.size - 1

    while ((messages(ind).sequence >= sig.seq_range._1) && (ind > 0)) {
      if (messages(ind).sequence <= sig.seq_range._2)
        sig.query ! messages(ind)

      ind -= 1
    }

    if (sig.seq_range._1 < messages.first.sequence)
      forward(((sig.seq_range._1,  messages.first.sequence)))

    else
      return sig.query ! QueryEOFSig()

  }


  override def exceptionHandler = {
    case e: Exception => Fyrehose.fatal("MessageCache / " + e.toString)
  }

}
