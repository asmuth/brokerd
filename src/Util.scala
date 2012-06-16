package com.paulasmuth.fyrehose

object FyrehoseUtil{

  def now() : Long =
    (new java.util.Date()).getTime / 1000


  def get_uuid() : String =
    java.util.UUID.randomUUID.toString


  def pfunc_unit : PartialFunction[Int, Unit] =
    { case _ => () }

}
