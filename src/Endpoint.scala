package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import java.net._
import java.io._
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.CharBuffer

class Endpoint(multixplex: Multiplex, channel: SocketChannel) extends Actor{

  case class StreamingMode
  case class QueryMode
  var mode = StreamingMode

  def act() = { 
    Actor.loop{ react{
      case buf: Array[Byte] => read(buf)
    }}
  }


  private def write(buf: Array[Byte]) = 
    multixplex.push(channel, buf)


  private def read(buf: Array[Byte]) = {
    println(new String(buf))
    // try{
    //   val event = new Event(buf)
    //   println("received: " + new String(event.bytes))
    // } catch {
    //   case e: com.google.gson.JsonParseException => error("invalid json")
    // }
  }

  private def error(msg: String) = {
    Fyrehose.error("endpoint closed: " + msg)
    write(("{\"error\": \""+msg+"\"}").getBytes)
    println("FIXPAUL: close connection")
  }

}