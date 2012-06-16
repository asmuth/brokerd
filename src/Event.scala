package com.paulasmuth.fyrehose

import com.google.gson._
import java.io._

class Event(raw: Array[Byte]){

  var touched = false 
  val root = EventParser.parse(raw)


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


  def exists(key: FQL_KEY) : Boolean = 
    root.get(key.get) != null // FIXPAUL: recurse


  def getAsString(key: FQL_KEY) : String =
    getAsGsonPrimitive(key).getAsString()

  def getAsInteger(key: FQL_KEY) : Int =
    getAsGsonPrimitive(key).getAsInt()

  def getAsDouble(key: FQL_KEY) : Double =
    getAsGsonPrimitive(key).getAsDouble()


  private def getAsGsonPrimitive(key: FQL_KEY) =
    root.get(key.get) // FIXPAUL: recurse

  private def serialize() : Array[Byte] =
    root.toString.getBytes

}
