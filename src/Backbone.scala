package com.paulasmuth.fyrehose

import java.util.concurrent._
import scala.actors.Actor
import scala.actors.Actor._

case class EventBody(raw: Array[Byte])

class Backbone() extends Actor{

  val runner  = Executors.newFixedThreadPool(Fyrehose.NUM_THREADS_PARSER)
  val queries = scala.collection.mutable.Set[Query]()

  def act() = { 
    Actor.loop{ react{
      case ev_body: EventBody => parse(ev_body)
      case event: Event => dispatch(event)
      case query: Query => execute(query)
    }}
  }


  private def dispatch(event: Event) = 
    println("received: " + new String(event.bytes))



  private def execute(query: Query) = {
    queries += query
    query.start()
  }


  private def finish(query: Query) = {
    queries -= query
    query ! HangupSig
  }


  private def parse(ev_body: EventBody) = 
    runner.execute(new Runnable { def run = {
      try{
        Fyrehose.backbone ! new Event(ev_body.raw)
      } catch {
        case e: ParseException => Fyrehose.error(e.toString)
      }
    }})


}