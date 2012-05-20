package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

class QueryBody(_raw: Array[Byte]){
  def raw = _raw
}

object QueryParser{
  
  def parse(bdy: QueryBody) : Query = {
  	println("QUERY CONSTRUCTED")
    val qry = new Query(bdy.raw)
    qry.start()
    return qry
  }

}