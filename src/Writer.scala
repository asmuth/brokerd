package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import java.io.FileWriter;

class Writer extends Actor {

  def act(){ 
    Actor.loop{ react{
      case msg: Message => persist(msg)
    }}
  }

  def persist(msg: Message) = {
    val file = 
      Fyrehose.CONFIG('out_dir) + "/" + ((msg.time() / 
      Fyrehose.FILE_CHUNK_SIZE) * 
      Fyrehose.FILE_CHUNK_SIZE).toString

    using (new FileWriter(file, true)){ 
      writer => writer.write(new String(msg.bytes) + "\n")
    }
  }

  def using[A <: {def close(): Unit}, B](param: A)(f: A => B): B =
    try { f(param) } finally { param.close() }

  override def exceptionHandler = {
    case e: Exception => Fyrehose.fatal(e.toString)
  }

}
