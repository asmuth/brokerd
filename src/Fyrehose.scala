package com.paulasmuth.fyrehose

import scala.collection.mutable.HashMap;
import java.util.Locale
import java.util.Date
import java.text.DateFormat

// todo:
//   conn-header: keepalive + safe_mode
//   check if out dir exists on start
//   listen-udp / upstream
//   fql: mkeyops, allcaps, less/greater-than, regex, except, only, \. unescape, smart typecasting

object Fyrehose{

  val VERSION = "v0.0.6"
  val CONFIG  = HashMap[Symbol,String]()

  var CONN_IDLE_TIMEOUT    = 5000
  var CONN_MAX_QUEUE_SIZE  = 20000
  val BUFFER_SIZE_PARSER   = 8192 * 4
  val BUFFER_SIZE_TCP      = 2048
  val BUFFER_SIZE_UDP      = 65535
  val MESSAGE_CACHE_SIZE   = 100000
  val FILE_CHUNK_SIZE      = 3600 * 6
  val DEFAULT_OUT_DIR      = "/tmp/fyrehose"

  var backbone : Backbone  = null
  var writer   : Writer    = null

  val message_cache = new MessageCache
  val message_index = new MessageIndex

  def main(args: Array[String]) : Unit = {
    var n = 0

    while (n < args.length) {

      if((args(n) == "-l") || (args(n) == "--listen-tcp"))
        { CONFIG += (('listen_tcp, args(n+1))); n += 2 }

      else if((args(n) == "-u") || (args(n) == "--listen-udp"))
        { CONFIG += (('listen_udp, args(n+1))); n += 2 }

      else if((args(n) == "-p") || (args(n) == "--path"))
        { CONFIG += (('out_dir, args(n+1))); n += 2 }

      else if((args(n) == "-t") || (args(n) == "--timeout"))
        { CONFIG += (('timeout, args(n+1))); n += 2 }

      else if((args(n) == "-x") || (args(n) == "--upstream"))
        { CONFIG += (('upstream, args(n+1))); n += 2 }

      else {
        println("error: invalid option: " + args(n) + "\n")
        return usage(false)
      }

    }

    if (
      (CONFIG contains 'listen_tcp unary_!) &&
      (CONFIG contains 'listen_udp unary_!)
    ) return usage()

    if (CONFIG contains 'out_dir unary_!)
      CONFIG += (('out_dir, DEFAULT_OUT_DIR))

    if (CONFIG contains 'upstream)
      return println("not yet implemented: -x / --upstream")

    if (CONFIG contains 'timeout)
      CONN_IDLE_TIMEOUT = CONFIG('timeout).toInt

    boot()
  }


  def boot() : Unit = {
    log("fyerhosed " + VERSION + " booting...")
    System.setProperty("actors.enableForkJoin", "false")

    backbone = new Backbone()
    backbone.start()

    writer = new Writer()
    writer.start()

    message_cache.start()
    message_index.start()

    if (CONFIG contains 'listen_tcp) {
      try{
        CONFIG('listen_tcp).toInt
      } catch { case e: NumberFormatException =>
        return println("error: invalid port: tcp/" + CONFIG('listen_tcp))
      }

      val tcp_listener = new TCPListener(CONFIG('listen_tcp).toInt)
      tcp_listener.listen
    }

    if (CONFIG contains 'listen_udp) {
      try{
        CONFIG('listen_udp).toInt
      } catch { case e: NumberFormatException =>
        return println("error: invalid port: udp/" + CONFIG('listen_udp))
      }

      val udp_listener = new UDPListener(CONFIG('listen_udp).toInt)
      udp_listener.listen
    }

  }


  def usage(head: Boolean = true) = {
    if (head)
      println("fyrehosed " + VERSION + " (c) 2012 Paul Asmuth\n")

    println("usage: fyrehose [options]                                                  ")
    println("  -l, --listen-tcp  <port>    listen for clients on this tcp port          ")
    println("  -u, --listen-udp  <port>    listen for clients on this udp port          ")
    println("  -p, --path        <path>    path to store data (default: /tmp/fyrehose/) ")
    println("  -t, --timeout     <msecs>   connection idle timeout (default: 5000ms)    ")
    // println("  -x, --upstream    <addr>    pull events from this fyrehosed            \n")
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
