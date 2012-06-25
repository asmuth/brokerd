package com.paulasmuth.fyrehose

import scala.collection.mutable.HashMap

class SequenceIndex(bucket_size: Int) {

  val sindex = HashMap[Long, (Int, Int)]()

  def next(msg: Message) : Unit = {
    val bucket = bucket_at(msg.time)

    if (sindex contains bucket unary_!) {
      sindex += ((bucket, ((msg.sequence, msg.sequence))))
      return ()
    }

    if (msg.sequence < sindex(bucket)._1)
      sindex += ((bucket, ((msg.sequence, sindex(bucket)._2))))

    if (msg.sequence > sindex(bucket)._2)
      sindex += ((bucket, ((sindex(bucket)._1, msg.sequence))))

  }


  def bucket_at(time: Long) =
    (time / bucket_size) * bucket_size


  def buckets_in(since: Long, until: Long) =
    new Range((since / bucket_size).toInt, (until / bucket_size).toInt, 1)
      .toList.map { x => x * bucket_size }


  def seq_range(since: FQL_TUNIX, until: FQL_TUNIX) : (Int, Int) =
    (((-1,0)) /: buckets_in(since.get, (
      if (until == null) FyrehoseUtil.now else until.get)))(
        (seqr: (Int, Int), buck: Int) => ((
          (if (
           (sindex contains buck) &&
           ((seqr._1 == -1) ||
           (sindex(buck)._1 < seqr._1)))
             sindex(buck)._1 
          else
            seqr._1),
          (if
            ((sindex contains buck) && 
            (sindex(buck)._2 > seqr._2))
              sindex(buck)._2
          else
            seqr._2))))


}
