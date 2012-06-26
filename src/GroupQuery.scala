package com.paulasmuth.fyrehose

import scala.actors._
import scala.collection.mutable.HashMap

class GroupQuery() extends Query {

  var group_key : FQL_KEY = null
  var groups = HashMap[String, Int]().withDefaultValue(0)


  def data(msg: Message) =
    if (matches(msg) && msg.exists(group_key.get)) {
      val elem = msg.getAsGsonPrimitive(group_key.get)

      if (elem.isJsonPrimitive)
        groups(msg.getAsString(group_key)) += 1

      else if (msg.getAsGsonPrimitive(group_key.get).isJsonArray) {
        val iter = msg.getAsGsonPrimitive(group_key.get).getAsJsonArray.iterator
        while (iter.hasNext)
          groups(iter.next.getAsString) += 1
      }
    }


  def eval(token: FQL_TOKEN) =
    throw new ParseException("invalid query token: " + token.getClass.getName)


  override def finish = {
    val json_key = (group_key.get.head /: group_key.get.tail)((m, s) => m + "." + s)

    groups.foreach ( t => recv ! QueryResponseChunk((
      "{ \"" + json_key + "\": " + t._1 + ", \"count\": " + t._2.toString + "}\n"
    ).getBytes))

    recv ! QueryExitSig(this)
  }


  override def assert = {

    until match {
      case t: FQL_TSTREAM =>
        throw new ParseException("can't GROUP over unbound time range (UNTIL STREAM)")
      case _ => ()
    }

    cmd.arg1 match {
      case k: FQL_KEY => group_key = k
      case _ => throw new ParseException("invalid argument for GROUP: " + cmd.arg1.getClass.getName)
    }

  }


}
