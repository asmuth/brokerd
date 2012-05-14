package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import java.net._
import java.io._
import java.nio.channels.SocketChannel

class Endpoint(multixplex: Multiplex, channel: SocketChannel) extends Actor{

  println("endpoint started")

  def act() = { 
    Actor.loop{ react{
      case msg: String => multixplex.push(channel, "FFFFU: " + msg)
    }}
  }

}