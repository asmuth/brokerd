package com.paulasmuth.fyrehose

import java.util.concurrent._
import scala.actors.Actor
import scala.actors.Actor._

case class EventBody(raw: Array[Byte])

class Backbone() extends Actor{

  val dispatch_pool  = Executors.newFixedThreadPool(Fyrehose.NUM_THREADS_DISPATCH)
  val queries = scala.collection.mutable.Set[Query]()
  var sequence = new java.util.concurrent.atomic.AtomicInteger

  val writer  = new Writer()
  writer.start()


  def act() = {
    Actor.loop{ react{
      case query: Query => execute(query)
      case event: Event => dispatch(event)
      case QueryExitSig(query) => finish(query)
    }}
  }

  private def dispatch(event: Event) = {
    println("dispatch scheduled")
    sequence.incrementAndGet();
    dispatch_pool.execute(new Runnable { def run = {
      println("dispatched: " + queries.size.toString)
      queries.foreach(_ ! event)
      writer ! event
    }})
  }


  private def execute(query: Query) = {
    println("EXECUTE EXEC START")
    queries += query
    query.sequence = sequence.get()
    query.start()
    println("EXECUTE EXEC STOP") 
  }


  private def finish(query: Query) = {
    println("FINISH EXEC START")
    queries -= query
    query ! HangupSig
    println("FINISH EXEC STOP")
  }


  override def exceptionHandler = {
    case e: Exception => Fyrehose.fatal("backbone / " + e.toString)
  }

}
