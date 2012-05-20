package com.paulasmuth.fyrehose

import java.util.concurrent._
import scala.actors.Actor
import scala.actors.Actor._


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
        Fyrehose.backbone ! ev_body.parse
      } catch {
        case e: com.google.gson.JsonParseException => Fyrehose.error("invalid json")
      }
    }})


}