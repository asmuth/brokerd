package com.paulasmuth.fyrehose

import java.util.concurrent._
import java.io._
import java.net._

class Listener(port: Int){

   val sock = new ServerSocket(port)
   val clients = Executors.newFixedThreadPool(Fyrehose.NUM_THREADS_DISPATCH)

   Fyrehose.log("listening on port " + port.toString())

   var conn_opened = new java.util.concurrent.atomic.AtomicInteger;
   var conn_closed = new java.util.concurrent.atomic.AtomicInteger;

   def listen = {
     clients.execute(new Runnable{ def run = {
       while(true){
         println(
           "[STATUS] open: " + conn_opened.get.toString + 
           ", closed: " + conn_closed.get.toString +
           ", diff: " + (conn_opened.get - conn_closed.get).toString
         )

         val conn = new Endpoint(sock.accept())
         clients.execute(conn)
       }
     }})
   }

}
