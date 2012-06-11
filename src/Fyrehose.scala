package com.paulasmuth.fyrehose

import java.util.Locale
import java.util.Date
import java.text.DateFormat

// todo:
//   conn-header: keepalive + safe_mode
//   filter key recursion via dot
//   query no kill on no match issue
//   query leading whitespace issue

object Fyrehose{

  val CONN_IDLE_TIMEOUT    = 1000
  val NUM_THREADS_PARSER   = 6
  val NUM_THREADS_DISPATCH = 6
  val BUFFER_SIZE_PARSER   = 8192 * 4
  val BUFFER_SIZE_SOCKET   = 2048
  val FILE_CHUNK_SIZE      = 3600 * 6

  var out_dir = "/tmp/fyrehose"

  val backbone = new Backbone()
  backbone.start()

  System.setProperty("actors.enableForkJoin", "false")

  def main(args: Array[String]) : Unit = {
    log("fyerhosed v0.0.2-dev booting...")

    val listener = new Listener(2323)
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
