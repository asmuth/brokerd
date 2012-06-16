package com.paulasmuth.fyrehose;

trait FQL_BUFFER {
  var cur : Char = 0
  var buf : String = null
}

trait FQL_TOKEN extends FQL_BUFFER {
  def ready : Boolean
  def next : FQL_TOKEN
  def buffer(_cur: Char, _buf: String) : String =
    if (ready) "" else _buf + _cur
  def next(_cur: Char, _buf: String) : FQL_TOKEN =
    { cur=_cur; buf = _buf; next }
}

trait FQL_VAL {}
trait FQL_META {}
trait FQL_OP {}

class FQL_ATOM extends FQL_TOKEN {
  def ready =
    ((cur == ' ') || (cur == '(')) && (buf.size > 0)
  def next =
    if (ready unary_!)
      this
    else buf.trim match {
      case "stream"    => new FQL_STREAM
      case "and"       => new FQL_AND
      case "or"        => new FQL_OR
      case "where"     => new FQL_WHERE(true)
      case "where_not" => new FQL_WHERE(false)
      case _ => throw new ParseException("invalid atom: " + buf)
    }
}

class FQL_OPERATOR extends FQL_TOKEN with FQL_OP with FQL_META {
  def ready = next != this
  def next = buf.trim match {
    case "=" => new FQL_OPERATOR_EQUALS
    case _   => this
  }
}

class FQL_VALUE extends FQL_TOKEN with FQL_VAL with FQL_META {
  def ready = next != this
  def next = cur match {
    case ' ' => this
    case _   => new FQL_KEY
  }
}

trait FQL_STATEMENT {
  def next(token: FQL_TOKEN) : FQL_TOKEN
}

trait FQL_KEYWORD extends FQL_BUFFER {
  def ready : Boolean =
    ((cur == ' ' && (buf.size == 0)) || buf == "()")
  def next : FQL_TOKEN =
    this.asInstanceOf[FQL_TOKEN]
}


class FQL_STREAM extends FQL_TOKEN with FQL_KEYWORD {}
class FQL_OR extends FQL_TOKEN with FQL_KEYWORD {}
class FQL_AND extends FQL_TOKEN with FQL_KEYWORD {}


trait FQL_OPERATOR_VARARG extends FQL_TOKEN with FQL_OP {
  var args : Int
  def ready = args == 0
  def next = if (args == 0) this else
    { args -= 1; new FQL_VALUE }
}

trait FQL_OPERATOR_UNARY extends FQL_TOKEN with FQL_OP {
  def ready = true
  def next = this
}

trait FQL_OPERATOR_BINARY extends FQL_OPERATOR_VARARG {
  var args : Int = 1
}

class FQL_OPERATOR_EQUALS extends FQL_OPERATOR_BINARY {}


class FQL_KEY extends FQL_TOKEN with FQL_VAL {
  def ready =
    (cur == ' ') || (cur == ')') || (cur == '=')
  def next = this
}

class FQL_WHERE(negated: Boolean) extends FQL_TOKEN with FQL_STATEMENT {
  var key : FQL_VAL  = null
  var op  : FQL_OP   = null

  override def buffer(_cur: Char, _buf: String) =
    if ((buf == "(") || (buf == ")"))
      ""
    else
      (_buf + _cur).trim

  def ready =
    (key != null) && (op != null) && (buf == ")")

  def next =
    if ((key == null) && (buf == "("))
      new FQL_VALUE
    else
      this

  def next(t: FQL_TOKEN) = t match {
    case k: FQL_VAL =>
      if (key == null)
        { key = k; new FQL_OPERATOR }
      else
        { /* is value */ this }
    case o: FQL_OP =>
      { op = o; this }
  }

}


