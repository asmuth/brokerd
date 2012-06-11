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


  def exists(key: String) : Boolean = 
    root.get(key) != null // FIXPAUL: recurse with dot operator


  def getAsString(key: String) : String =
    root.get(key).getAsString() // FIXPAUL: recurse with dot operator


  private def serialize() : Array[Byte] =
    root.toString.getBytes

}
