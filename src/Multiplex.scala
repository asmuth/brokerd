package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.CharBuffer
import java.nio.channels._
import java.nio.charset.Charset

class Multiplex() extends Runnable {
  val listener:ServerSocketChannel = ServerSocketChannel.open()
  val selector:Selector = Selector.open()

  var endpoints = HashMap[SocketChannel, Endpoint]()

  case class Stream(channel: SocketChannel, msg: String)
  case class Read(channel: SocketChannel)
  case class Select()

  val reactor = actor { loop { 
    receive {
      case Select => select()
      case Stream(channel, msg) => stream(channel, msg)
      case Read(channel) => ready(channel)
    }
  }}

  def push(channel: SocketChannel, msg: String){
    reactor ! Stream(channel, msg)
    selector.wakeup()
  }

  def run() {
    //val port = Integer.parseInt(node.listen.split(":", 2)(1))
    val port = 2323

    reactor.start()
    listener.socket().bind(new InetSocketAddress(port))
    listener.configureBlocking(false)
    listener.register(selector, SelectionKey.OP_ACCEPT)

    reactor ! Select
  }

  private def stream(channel: SocketChannel, msg: String){
    println("stream start")
    channel.configureBlocking(false)
    channel.register(selector, SelectionKey.OP_WRITE, msg)
  }

  def select() {
    println("SELECT START")

    selector.select()

    selector.selectedKeys().foreach { key =>

      if (key.isValid() unary_!){
        println("DISCONNECT CHANNEL CLOSED")
      }
      
      else if (key.isAcceptable()) {
        println("SELECT ACCEPTABLE")
        accept(key)
      } 

      else if (key.isReadable()) {
        println("SELECT READABLE")
        read(key)
      } 

      else if (key.isWritable()) {
        println("SELECT WRITABLE")
        write(key)
      } 

    }

    println("SELECT END")
  }

  def accept(key: SelectionKey) {
    val socket:ServerSocketChannel = key.channel().asInstanceOf[ServerSocketChannel]

    socket.accept() match {
      case null => ()   
      case channel:SocketChannel => {
        println("connection opened")
        val endpoint = new Endpoint(this, channel)
        endpoint.start()
        endpoints += ((channel, endpoint))
        ready(channel)
      }
    }

    reactor ! Select
  }

  def ready(channel: SocketChannel) {
    channel.configureBlocking(false)
    channel.register(selector, SelectionKey.OP_READ, "fnord")
  }

  def read(key: SelectionKey) {
    val channel: SocketChannel = key.channel().asInstanceOf[SocketChannel]
    val buf: ByteBuffer = ByteBuffer.allocate(1024)
    
    if (channel.isOpen unary_!){
      println("DISCONNECT CHANNEL CLOSED")
    }

    else channel.read(buf) match {

      case 0 => ()

      case -1 => {
        println("DISCONNECT END OF STREAM")
        key.cancel()
      }

      case m => {
        buf.flip()

        // FIXPAUL: check if message ends with newline, otherwise -> problem
        val msg = Charset.forName("UTF-8").decode(buf).toString().trim()
        println("READ RECEIVED, REALYING TO ACTOR: " + msg)

        if(endpoints contains channel){
          endpoints(channel) ! msg
        } else {
          println("!!!! CAN'T FIND ENDPOINT")
        }

        buf.compact()
      }
      
    }
  }

  def write(key: SelectionKey){
    val channel: SocketChannel = key.channel().asInstanceOf[SocketChannel]

    if(key.attachment == null){
       println("CANT WRITE - NO ATTACHMENT")
    }

    else {
      channel.write(
        Charset.forName("UTF-8").encode(
          CharBuffer.wrap(key.attachment.asInstanceOf[String] + "\n")
        )
      )

      ready(channel)
    }
  }


  //   } catch {
  //       case e: CancelledKeyException => println("CANNCELLED KEY")
  //       case e: ClosedChannelException => println("CLOSED CHANNEL")
  //   }

}