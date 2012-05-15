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

  def act() = { 
    Actor.loop{ react{
      case buf: Array[Byte] => read(buf)
    }}
  }


  def write(buf: Array[Byte]) = 
    multixplex.push(channel, buf)


  def read(buf: Array[Byte]) = {
    // FIXPAUL: check if message ends with newline, otherwise ->   problem
    val msg = new String(buf, "UTF-8")

    println("endpoint read: " + msg)
    write("FNORD?".getBytes("UTF-8"))
    write("FNORD!\n".getBytes("UTF-8"))
  }

}