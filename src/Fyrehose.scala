package com.paulasmuth.fyrehose

import java.util.Locale
import java.util.Date
import java.text.DateFormat

// todo:
//   > multiple streams kill each other since attachment will be overwritten. fix with sta
//   > move endpoints hash to main object since multiplex will have it's own thread
//   > kill endpoints on connection close!

object Fyrehose{

  val NUM_THREADS_PARSER  = 12
  val BUFFER_SIZE_PARSER  = 4096
  val BUFFER_SIZE_SOCKET  = 1024

  val backbone = new Backbone()
  backbone.start()
  
  def main(args: Array[String]) : Unit = {
    println("hello fyrehose")

    val multiplex = new Multiplex()
    multiplex.run()
  }

  def log(msg: String) = {
    val now = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.FRANCE)
    println("[" + now.format(new Date()) + "] " + msg)
  }

  def error(msg: String) =
    log("[ERROR] " + msg)

}