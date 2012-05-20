package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels._

import java.nio.charset.Charset
import java.nio.CharBuffer
import java.nio.charset.Charset


class Multiplex() extends Runnable {

  case class Stream(channel: SocketChannel, buf: Array[Byte])
  case class Read(channel: SocketChannel)
  case class Hangup(channel: SocketChannel)
  case class Select()


  val listener  = ServerSocketChannel.open()
  val selector  = Selector.open()
  val endpoints = HashMap[SocketChannel, Endpoint]()
  val stack     = HashMap[SocketChannel, ListBuffer[Array[Byte]]]()


  val reactor = actor { loop { 
    receive {
      case Select => select()
      case Stream(channel, buf) => stream(channel, buf)
      case Read(channel) => ready(channel)
      case Hangup(channel) => channel.close()
    }
  }}


  def push(channel: SocketChannel, buf: Array[Byte]){
    reactor ! Stream(channel, buf)
    selector.wakeup()
  }

  def hangup(channel: SocketChannel){
    reactor ! Hangup(channel)
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


  private def stream(channel: SocketChannel, buf: Array[Byte]){
    if(stack contains channel unary_!){
      stack(channel) = ListBuffer[Array[Byte]]()
    }

    stack(channel) += buf

    channel.configureBlocking(false)
    channel.register(selector, SelectionKey.OP_WRITE, null)
  }


  def select() {
    selector.select()
    selector.selectedKeys().foreach { key =>

      if (key.isValid() unary_!)
        println("DISCONNECT CHANNEL CLOSED")
      
      else if (key.isAcceptable())
        accept(key)

      else if (key.isReadable())
        read(key)

      else if (key.isWritable())
        write(key)

    }
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
    val buf: ByteBuffer = ByteBuffer.allocate(Fyrehose.BUFFER_SIZE_SOCKET)
    
    if (channel.isOpen unary_!){
      println("DISCONNECT CHANNEL CLOSED")
    }

    else channel.read(buf) match {

      case 0 => ()

      case -1 => {
        key.cancel()

         if(endpoints contains channel)
           endpoints(channel) ! HangupSig
      }

      case m => {
        buf.flip()

        if(endpoints contains channel){

          val bytes = new Array[Byte](buf.remaining());
          buf.get(bytes, 0, bytes.length);

          endpoints(channel) ! bytes
        } else {
          println("!!!! CAN'T FIND ENDPOINT")
        }

        buf.compact()
      }
      
    }
  }


  def write(key: SelectionKey){
    val channel: SocketChannel = key.channel().asInstanceOf[SocketChannel]
    val buf: ByteBuffer = ByteBuffer.allocate(Fyrehose.BUFFER_SIZE_SOCKET)


    if(stack contains channel unary_!){
       println("CANT WRITE - NO ATTACHMENT")
    }

    else {
      stack(channel).foreach{ chunk => buf.put(chunk) }
      stack -= channel
      buf.flip()
      channel.write(buf)
      ready(channel)
    }
  }


}