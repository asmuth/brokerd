package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.HashMap

class MessageIndex extends Actor {

  val base_size = 10 // FIXPAUL

  val index_1n     = new SequenceIndex(base_size * 1)
  val index_10n    = new SequenceIndex(base_size * 10)
  val index_100n   = new SequenceIndex(base_size * 100)
  val index_1000n  = new SequenceIndex(base_size * 1000)
  val index_5000n  = new SequenceIndex(base_size * 5000)
  val index_20000n = new SequenceIndex(base_size * 20000)

  def act = loop { react {
    case m: Message => next(m)
  }}


  def next(msg: Message) : Unit = {
    index_1n.next(msg)
    index_10n.next(msg)
    index_100n.next(msg)
    index_1000n.next(msg)
    index_5000n.next(msg)
  }


  def seq_range(since: FQL_TUNIX, until: Int) : (Int, Int) =
    ((seq_range(since, null)._1, until))


  def seq_range(since: FQL_TUNIX, until: FQL_TUNIX) : (Int, Int) = {
    val range = if (until == null)
      FyrehoseUtil.now - since.get
    else
      until.get - since.get

    println("range: " + range.toString)

    if (range < (10 * base_size * 2))
      index_1n.seq_range(since, until)

    else if (range < (100 * base_size * 2))
      index_10n.seq_range(since, until)

    else if (range < (1000 * base_size * 2))
      index_100n.seq_range(since, until)

    else if (range < (5000 * base_size * 2))
      index_1000n.seq_range(since, until)

    else if (range < (20000 * base_size * 2))
      index_5000n.seq_range(since, until)

    else
      index_20000n.seq_range(since, until)

  }


  override def exceptionHandler = {
    case e: Exception => Fyrehose.fatal("MessageIndex / " + e.toString)
  }


}
