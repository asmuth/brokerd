package com.paulasmuth.fyrehose

import java.util.Locale
import java.util.Date
import java.text.DateFormat

// TODO:
// close endpoint via timeout
// conn-header: keepalive + safe_mode

object Fyrehose{

  val CONN_IDLE_TIMEOUT   = 5000
  val NUM_THREADS_PARSER  = 12
  val BUFFER_SIZE_PARSER  = 16276
  val BUFFER_SIZE_SOCKET  = 4096

  val backbone = new Backbone()
  backbone.start()
  
  def main(args: Array[String]) : Unit = {
    log("fyerhosed v0.0.1-dev booting...")

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