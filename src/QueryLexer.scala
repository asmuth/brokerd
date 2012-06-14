package com.paulasmuth.fyrehose

import scala.util.matching.Regex

case class X_TOKEN(name: Symbol, key: Symbol = null, value: Any = null) {}

trait QueryLexer {

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
      val keyword = keywords
        .find(k => s.matches("^" + k.name + "$"))

      if (keyword isDefined)
        return Some(new X_TOKEN(keyword.get))
      else
        return None
    }

  }

  object X_COMMAND extends X_ATOM {

    var commands = Map[Symbol, Class[_ <: Any]]()

    def define(k: Symbol, klass: Class[_ <: Any]) =
      commands += ((k, klass))

    def unapply(s: String) : Option[X_TOKEN] = {

      val command = commands
        .find(k => s.matches("^" + k._1.name + "$"))

      if (command isDefined)
        return Some(new X_TOKEN(command.get._1, null, command.get._2))
      else
        return None
    }

  }

  object X_WHERE extends X_ATOM {

    var filters = Map[Symbol, String]()

    def define(k: Symbol, r: String) =
      filters += ((k, r))

    def unapply(s: String) : Option[X_TOKEN] = {
      val command = filters
        .find(k => s.matches("^" + k._1.name + "$"))

      for (filter <- filters) {

        val x_true  = ("""^where\(([^ ]+)""" + filter._2 + """\)$""").r
        val x_false = ("""^where_not\(([^ ]+)""" + filter._2 + """\)$""").r

        s match {
          case x_true(k: String, v: String) =>
            return Some(new X_TOKEN('where, Symbol(k), v))
          case x_false(k: String, v: String) =>
            return Some(new X_TOKEN('where_not, Symbol(k), v))
          case _ =>
            ()
        }

      }

      return None
    }

  }

//  class X_EQUALS_STR extends X_WHERE {
//    def regex = """ *= *'([^']*)'\)$"""
//    def eval(v: String) = (x: String) => x == v
//  }

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


}

