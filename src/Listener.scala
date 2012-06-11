package com.paulasmuth.fyrehose

import java.util.concurrent._
import java.io._
import java.net._

class Listener(port: Int){

  val sock = new ServerSocket(port)
  val clients = Executors.newCachedThreadPool() // evil ~paul

  Fyrehose.log("listening on port " + port.toString())

  def listen = {
    clients.execute(new Runnable{ def run = {
      while(true){
        try{
          val conn = new Endpoint(sock.accept())
          clients.execute(conn)
        } catch {
          case e: Exception => Fyrehose.fatal(e.toString)
        }
      }
    }})
  }

}
