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
    case sig: QueryDiscoverSig => retrieve_async(sig)
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

    if (messages.size > cache_size)
      trim

  }


  def forward(seq_range: (Int, Int)) = ()
    //println("FIXPAUL foward " + seq_range.toString)


  def size =
    messages.size


  def retrieve_async(sig: QueryDiscoverSig) =
    retrieve(messages.toArray, sig) // FIXPAUL: in threadpool!


  def retrieve(cpy: Array[Message], sig: QueryDiscoverSig) : Unit = {
    var seq_range : (Int, Int) = sig.seq_range

    if (seq_range == ((-1, 0)))
      return sig.query ! QueryEOFSig()

    if (seq_range._1 == -1)
      seq_range = ((0, seq_range._2))

    if (seq_range._2 < cpy.first.sequence)
      return forward(seq_range)

    var ind = cpy.size - 1

    while ((cpy(ind).sequence >= seq_range._1) && (ind > 0)) {
      if (cpy(ind).sequence <= seq_range._2)
        sig.query ! cpy(ind)

      ind -= 1
    }

    if ((seq_range._1 < cpy.first.sequence) && (seq_range._1 > 0))
      forward(((seq_range._1,  cpy.first.sequence)))

    else
      return sig.query ! QueryEOFSig()

  }


  override def exceptionHandler = {
    case e: Exception => Fyrehose.fatal("MessageCache / " + e.toString)
  }

}
