package com.paulasmuth.fyrehose

import java.lang.management.ManagementFactory
import java.lang.management.MemoryPoolMXBean
import java.lang.management.MemoryUsage

class ParseException(msg: String) extends Exception{
  override def toString = msg
}

object FyrehoseUtil{

  def now() : Long =
    now_ms / 1000


  def now_ms() : Long =
    (new java.util.Date()).getTime


  def get_uuid() : String =
    java.util.UUID.randomUUID.toString


  def pfunc_unit : PartialFunction[Int, Unit] =
    { case _ => () }


  def used_mem =
    ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() /
    (1024 * 1024).toDouble

}

