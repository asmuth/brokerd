package com.paulasmuth.fyrehose

import java.io._
import com.google.gson._

class Message(raw: Array[Byte]){

  var sequence = 0

  var touched = false 
  val root = MessageParser.parse(raw)


  if (root.has("_time") unary_!){
    touched = true
    root.addProperty("_time", FyrehoseUtil.now)
  }

  if (root.has("_eid") unary_!){
    touched = true
    root.addProperty("_eid", FyrehoseUtil.get_uuid)
  }


  def bytes() : Array[Byte] =
    if (touched) serialize() else raw


  def time() : Long =
    root.get("_time").getAsLong()


  def exists(keys: List[String]) : Boolean = try {
    getAsGsonPrimitive(keys) != null
  } catch {
    case e: ClassCastException => false
  }


  def getAsString(key: FQL_KEY) : String =
    getAsGsonPrimitive(key.get).getAsString()

  def getAsInteger(key: FQL_KEY) : Int =
    getAsGsonPrimitive(key.get).getAsInt()

  def getAsDouble(key: FQL_KEY) : Double =
    getAsGsonPrimitive(key.get).getAsDouble()

  def getAsBoolean(key: FQL_KEY) : Boolean =
    getAsGsonPrimitive(key.get).getAsBoolean()


  private def getAsGsonPrimitive(keys: List[String]) = {
    var parent = ((root /: keys.init)((t, k) =>
      if (t == null) null else t.getAsJsonObject(k)))

    if (parent == null)
      null
    else
      parent.get(keys.last)
  }


  private def serialize() : Array[Byte] =
    root.toString.getBytes


}
