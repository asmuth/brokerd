package com.paulasmuth.fyrehose

import java.util.concurrent._
import scala.actors.Actor
import scala.actors.Actor._

case class EventBody(raw: Array[Byte])

class Backbone() extends Actor{

  val queries = scala.collection.mutable.Set[Query]()
  var sequence = 0

  def act() = {
    Actor.loop{ receive{
      case query: Query => execute(query)
      case event: Event => dispatch(event)
      case QueryExitSig(query) => finish(query)
    }}
  }

  private def dispatch(event: Event) = {
    sequence += 1
    queries.foreach(_ ! event)
    Fyrehose.writer ! event
  }


  private def execute(query: Query) = {
    queries += query
    query.sequence = sequence
    query.start()
  }


  private def finish(query: Query) = {
    queries -= query
    query ! HangupSig
  }


  override def exceptionHandler = {
    case e: Exception => Fyrehose.fatal("backbone / " + e.toString)
  }

}
