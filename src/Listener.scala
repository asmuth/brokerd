package com.paulasmuth.fyrehose

import java.util.concurrent._
import java.io._
import java.net._
 
class Listener(port: Int){

  val clients = Executors.newCachedThreadPool() // FIXPAUL: evil!!!
  var sock : ServerSocket = null

  try{
    sock = new ServerSocket(port)
  } catch {
    case e: Exception => Fyrehose.fatal(e.toString)
  }

  Fyrehose.log("listening on port " + port.toString())

  def listen = {
    while(true){
      val conn = new Endpoint(sock.accept())
      clients.execute(conn)
    }
  }

}