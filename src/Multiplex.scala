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

  //val stack = HashMap[SocketChannel, ListBuffer[StreamSig]]()

  case class Select()
  case class Accept(key: SelectionKey)
  case class Read(key: SelectionKey)
  //case class Write(key: SelectionKey, sig: StreamSig)

  val multiplex = actor { loop { 
    react {
      case Select => select()
  //  case Accept(key) => accept(key)
  //       case Read(key) => safe_read(key)
  //       case Write(key, sig) => write(key, sig)
  //       case sig: (StreamSig, Actor) => queue(sig._1, sig._2)
  //       case sig: ConnectSig => {
  //         // println("!!!!CONNECTION ESTABLISHED!!!!")
  //         sig.channel.configureBlocking(false)
  //         sig.channel.register(selector, SelectionKey.OP_READ, sig.actor)
  //       }
    }
  }}

  def run() {
    //val port = Integer.parseInt(node.listen.split(":", 2)(1))
    val port = 2323

    multiplex.start()
    listener.socket().bind(new InetSocketAddress(port))
    listener.configureBlocking(false)
    listener.register(selector, SelectionKey.OP_ACCEPT)

    multiplex ! Select
  }

  // def stream(sig: StreamSig, endpoint: Actor){
  //   multiplex ! ((sig, endpoint))
  //   selector.wakeup()
  // }

  // def queue(sig: StreamSig, endpoint: Actor){
  //   if(sig.channel.isOpen){
  //     if (stack contains sig.channel unary_!)
  //       stack(sig.channel) = ListBuffer[StreamSig]()

  //     stack(sig.channel) += sig

  //     // println("WRITE QUEUE APPEND, STARTING SELECT")

  //     sig.channel.configureBlocking(false)
  //     sig.channel.register(selector, SelectionKey.OP_WRITE, endpoint)

  //     multiplex ! Select
  //   } else {
  //     // println("MESSAGE TO DEAD CHANNEL, DROPPING")

  //     sig.channel.close()
  //   }
  // }

  def select() {
    println("SELECT START")

    selector.select()

    selector.selectedKeys().foreach { key =>

      if (key.isAcceptable()) {
        println("SELECT ACCEPTABLE")
        //multiplex ! Accept(key)
        accept(key)
      } 


      if (key.isReadable()) {
        println("SELECT READABLE")
        //multiplex ! Accept(key)
        read(key)
      } 

    }

    println("SELECT END")
  
  }

  def accept(key: SelectionKey) {
    val socket:ServerSocketChannel = key.channel().asInstanceOf[ServerSocketChannel]

    socket.accept() match {
      case null => ()   
      case channel:SocketChannel => {
        channel.configureBlocking(false)
        channel.register(selector, SelectionKey.OP_READ, "fnord")
      }
    }

    multiplex ! Select
  }

  //   

  //     try{
  //       

  //       else if (key.isConnectable()) {
  //         val channel = key.channel().asInstanceOf[SocketChannel]
  //         // println("OP_CONNECT")

  //         try{
  //           channel.finishConnect() 
  //           multiplex ! new ConnectSig(channel, key.attachment.asInstanceOf[Actor])
  //         } catch {
  //           case e: java.net.SocketException => {
  //             channel.close()
  //             key.attachment.asInstanceOf[Actor] ! ConnectionClosedSig
  //             node.logger ! "cannot establish uplink: " + e.getMessage()
  //           }
  //         }
  //       }

  //       else if (key.isReadable()) {
  //         // // println("SELECT READABLE")
  //         multiplex ! Read(key)
  //       }

  //       else if (key.isWritable()){
  //         val channel:SocketChannel = key.channel().asInstanceOf[SocketChannel]
  //         // // println("SELECT WRITEABLE")

  //         if ((stack contains channel) && (stack(channel).size > 0))
  //           self ! Write(key, stack(channel).remove(0))
  //       }

  //     } catch {
  //       case e: CancelledKeyException => println("CANNCELLED KEY")
  //       case e: ClosedChannelException => println("CLOSED CHANNEL")
  //     }
  //   }

  //   multiplex ! Select
  // }



  // def safe_read(key: SelectionKey){
  //   try{
  //     read(key)
  //   } catch {
  //     case e: Exception => {
  //       key.channel().asInstanceOf[SocketChannel].close()

  //       if (key.attachment() != null)
  //         key.attachment().asInstanceOf[Actor] ! ConnectionClosedSig
        
  //       node.logger ! "CONNECTION DIED: " + e.getMessage()
  //     }
  //   }
  // }
   
  def read(key: SelectionKey) {
    val channel: SocketChannel = key.channel().asInstanceOf[SocketChannel]
    val buf: ByteBuffer = ByteBuffer.allocate(1024)
    
    if (channel.isOpen unary_!){
      println("DISCONNECT")
    }

    else channel.read(buf) match {

      case 0 => ()

      case -1 => {
        println("DISCONNECT")
      }

      case m => {
        buf.flip()

        // FIXPAUL: check if message ends with newline, otherwise -> problem
        val msg = Charset.forName("UTF-8").decode(buf).toString().trim()
        println("READ RECEIVED, REALYING TO ACTOR: " + msg)

        buf.compact()
        // key.cancel()
        // key.attachment().asInstanceOf[Actor] ! new ReceiveSig(msg)  
      }
      
    }
  }

  // def write(key: SelectionKey, sig: StreamSig){
  //   val channel: SocketChannel = sig.channel()

  //   try{

  //     channel.write(
  //       Charset.forName("UTF-8").encode(
  //         CharBuffer.wrap(sig.msg() + "\n")
  //       )
  //     )

  //     if(key.attachment == null){
  //       println("CLOSING CONNECTION - NO ATTACHMENT")
  //       //channel.close()
  //     } 

  //     else {
  //       channel.configureBlocking(false)
  //       channel.register(selector, SelectionKey.OP_READ, key.attachment)
  //     }

  //   } catch {
  //       case e: CancelledKeyException => println("CANNCELLED KEY")
  //       case e: ClosedChannelException => println("CLOSED CHANNEL")
  //   }
  // }

}