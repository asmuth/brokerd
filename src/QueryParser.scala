package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._

case class QueryBody(raw: Array[Byte])

object QueryParser{
  
  def parse(bdy: QueryBody) : Query = {
  	println("QUERY CONSTRUCTED")
    val qry = new Query(bdy.raw)
    qry.start()
    return qry
  }

}