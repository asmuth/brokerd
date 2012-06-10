package com.paulasmuth.fyrehose

import scala.actors._

class InfoQuery(raw: String) extends Query{

  var recv : Actor = null

  def execute(endpoint: Actor) = {
    recv = endpoint

    recv ! QueryResponseChunk("fnord\n".getBytes)
    recv ! QueryExitSig(this)
  }

  def data(event: Event) = ()

}
