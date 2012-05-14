package com.paulasmuth.fyrehose

import java.util.Locale
import java.util.Date
import java.text.DateFormat

object Fyrehose{
  
  def main(args: Array[String]) : Unit = {
    println("hello fyrehose")

    val multiplex = new Multiplex()
    multiplex.run()
  }

  def log(msg: String) = {
    val now = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.FRANCE)
    println("[" + now.format(new Date()) + "] " + msg)
  }

}