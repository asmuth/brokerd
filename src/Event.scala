package com.paulasmuth.fyrehose

import com.google.gson._
import java.io._

class Event(raw: Array[Byte]){
  
  var touched = false 
  val root = parse()


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


  private def serialize() : Array[Byte] =
    root.toString.getBytes


  private def parse() : JsonObject = {
    (new JsonParser()).parse(
      new InputStreamReader(
        new ByteArrayInputStream(raw)))
    match {
      case obj: JsonObject => return obj
      case _ => throw (new JsonParseException("not an json-object"))
    }
  }
  
}