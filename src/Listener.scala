package com.paulasmuth.fyrehose

import java.util.concurrent._
import java.io._
import java.net._
 
class Listener {

   val sock = new ServerSocket(2323)
   val clients = Executors.newCachedThreadPool() // FIXPAUL: evil!!!

   def listen = {
     while(true){
       val conn = new Endpoint(sock.accept())
       clients.execute(conn)
     }
   }

}