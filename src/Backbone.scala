package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

class Backbone() extends Actor{

  def act() = { 
    Actor.loop{ react{
      case msg: String => println("backbone: " + msg)
    }}
  }

}