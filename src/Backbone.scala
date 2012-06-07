package com.paulasmuth.fyrehose

import java.util.concurrent._
import scala.actors.Actor
import scala.actors.Actor._

case class EventBody(raw: Array[Byte])

class Backbone() extends Actor{

  val parser_pool    = Executors.newFixedThreadPool(Fyrehose.NUM_THREADS_PARSER)
  val dispatch_pool  = Executors.newFixedThreadPool(Fyrehose.NUM_THREADS_DISPATCH)

  val queries = scala.collection.mutable.Set[Query]()

  val writer  = new Writer()
  writer.start()

  var sequence = 0

  def act() = { 
    Actor.loop{ react{
      case event: Event => dispatch(event)
      case query: Query => execute(query)
      case QueryExitSig(query) => finish(query)
    }}
  }


  def announce(ev_body: EventBody) =     
    parser_pool.execute(new Runnable { def run = {
      try{
        Fyrehose.backbone ! new Event(ev_body.raw)
      } catch {
        case e: ParseException => Fyrehose.error(e.toString)
      }
    }})


  private def dispatch(event: Event) = {
    sequence += 1    
    dispatch_pool.execute(new Runnable { def run = {
      queries.foreach(_ ! event)
      writer ! event
    }})
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
    case e: Exception => Fyrehose.fatal(e.toString)
  }
  
}