package com.paulasmuth.fyrehose

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.matching.Regex

object QueryParser{
  
  def parse(bdy: QueryBody) : Query = {

  	val x_stream = ".*select(.*).*".r

  	val query: Query = bdy.raw match {
  	  case x_stream => new PingPongQuery(bdy.raw)  
  	}
  	
  	query.start()
    return query
  }

}