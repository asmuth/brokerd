package com.paulasmuth.fyrehose

import scala.util.matching.Regex

case class X_TOKEN(key: Symbol, value: Any) {}

trait FQL {

  val X_VALIDATE = """^((([a_-z])+\([^\(]*\)|stream|and|or) *)+$"""
  val X_EXTRACT  = """([a_-z])+\([^\(]*\)|stream|and|or"""

  trait X_ATOM {
    def unapply(s: String) : Option[X_TOKEN]
  }

  object X_KEYWORD extends X_ATOM {

    var keywords = List[Symbol]()

    def define(k: Symbol) = 
      keywords = k :: keywords

    def unapply(s: String) : Option[X_TOKEN] = {
      //val extract = ("^" + symbol.name + "$").r
      //  .unapplySeq(s)

      val keyword = keywords.find(k => s.matches("^" + k.name + "$"))

      if (keyword isDefined)
        return Some(new X_TOKEN(keyword.get, ()))
      else
        return None
    }

  }

/*
  object X_WHERE extends X_ATOM {

    val filters = Map(
      """ *= *'([^']*)'\)$""" -> 'equals_str
    )

    def eval(v: String) : String => Boolean

    def unapply(s: String) : Option[X_TOKEN] = {
      val x_where_true = ("""^where\(([^ ]+)""" + regex).r
      val x_where_false = ("""^where_not\(([^ ]+)""" + regex).r
      s match {
        case x_where_true(k: String, v: String) =>
          return Some(new X_TOKEN(symbol, eval(v)))
        case x_where_false(k: String, v: String) =>
          return Some(new X_TOKEN(symbol, negated(eval(v))))
        case _ => 
          return None
      }
    }

    def negated(l: String => Boolean): String => Boolean =
      (x: String) => l(x) unary_!

  }


  trait X_WHERE extends X_TOKEN { }

  class X_EQUALS_STR extends X_WHERE {
    def regex = """ *= *'([^']*)'\)$"""
    def eval(v: String) = (x: String) => x == v
  }

//  object X_EQUALS_INT extends X_WHERE {
//    def symbol = 'equals_int
//    def regex = """ *= *([0-9]+)\)$"""
//    def eval(v: String) = (x: String) => x.toInt == v.toInt
//  }
//
//  object X_EQUALS_DBL extends X_WHERE {
//    def symbol = 'equals_dbl
//    def regex = """ *= *([0-9]+\.[0-9]+)\)$"""
//    def eval(v: String) = (x: String) => x.toDouble == v.toDouble
//  }
*/

}

