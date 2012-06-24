package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.HashMap

class MessageIndex extends Actor {

  val bucket_size    = 10 // FIXPAUL

  val sequence_index = HashMap[Long, (Int, Int)]()

  def act = loop { react {
    case m: Message => next(m)
  }}


  def next(msg: Message) : Unit = {
    println(sequence_index)
    val bucket = bucket_at(msg.time)

    if (sequence_index contains bucket unary_!) {
      sequence_index += ((bucket, ((msg.sequence, msg.sequence))))
      return ()
    }

    if (msg.sequence < sequence_index(bucket)._1)
      sequence_index += ((bucket, ((msg.sequence, sequence_index(bucket)._2))))

    if (msg.sequence > sequence_index(bucket)._2)
      sequence_index += ((bucket, ((sequence_index(bucket)._1, msg.sequence))))

  }


  def bucket_at(time: Long) =
    (time / bucket_size) * bucket_size


}
