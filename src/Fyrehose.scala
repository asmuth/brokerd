package com.paulasmuth.fyrehose

import java.util.Locale
import java.util.Date
import java.text.DateFormat

// todo:
//   conn-header: keepalive + safe_mode
//   filter key recursion via dot
//   query no kill on no match issue
//   query leading whitespace issue
//   make conn_idle_timeout configurable

object Fyrehose{

  val CONN_IDLE_TIMEOUT    = 5000
  val BUFFER_SIZE_PARSER   = 8192 * 4
  val BUFFER_SIZE_SOCKET   = 2048
  val FILE_CHUNK_SIZE      = 3600 * 6

  var out_dir = "/tmp/fyrehose"

  val backbone = new Backbone()
  backbone.start()

  val listener = new Listener(2323)

  System.setProperty("actors.enableForkJoin", "false")

  def main(args: Array[String]) : Unit = {
    log("fyerhosed v0.0.3-dev booting...")

    listener.listen
  }

  def log(msg: String) = {
    val now = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.FRANCE)
    println("[" + now.format(new Date()) + "] " + msg)
  }

  def error(msg: String) =
    log("[ERROR] " + msg)

  def fatal(msg: String) = {
    error(msg); System.exit(1)
  }

}
