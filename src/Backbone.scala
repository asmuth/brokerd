package com.paulasmuth.fyrehose

import java.util.concurrent._
import scala.actors.Actor
import scala.actors.Actor._

case class EventBody(raw: Array[Byte])

class Backbone() extends Actor{

  val runner = Executors.newFixedThreadPool(Fyrehose.NUM_THREADS_PARSER)

  def act() = { 
    Actor.loop{ react{
      case ev_body: EventBody => incoming(ev_body)
      case event: Event => dispatch(event)
    }}
  }


  private def dispatch(event: Event) = 
    println("received: " + new String(event.bytes))


  private def incoming(ev_body: EventBody) = 
    runner.execute(new Runnable { def run = {
      try{
        Fyrehose.backbone ! new Event(ev_body.raw)
      } catch {
        case e: ParseException => Fyrehose.error(e.toString)
      }
    }})


}