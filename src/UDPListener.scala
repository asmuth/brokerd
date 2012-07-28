package com.paulasmuth.fyrehose

import java.net.DatagramSocket
import java.net.DatagramPacket

class UDPListener(port: Int) extends Receivable {

  def listen = {
    Fyrehose.log("listening on udp/0.0.0.0:" + port.toString)

    var buf  = new Array[Byte](Fyrehose.BUFFER_SIZE_UDP)
    val next = new DatagramPacket(buf, Fyrehose.BUFFER_SIZE_UDP)
    val sock = new DatagramSocket(port)

    val stream = new InboundStream(this, Fyrehose.BUFFER_SIZE_UDP)
    stream.set_safe_mode(false)

    while (true) {
      sock.receive(next)
      stream.read(next.getData, Fyrehose.BUFFER_SIZE_UDP)
    }

  }

  def message(ev_body: MessageBody) = try{
    Fyrehose.backbone ! new Message(ev_body.raw)
  } catch {
    case e: ParseException => 
      Fyrehose.error("via UDP: " + e.toString)
  }


  def query(qry: QueryBody) =
    Fyrehose.error("received query via UDP: illegal")

}
