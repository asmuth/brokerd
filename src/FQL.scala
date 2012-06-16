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

trait FQL_STATEMENT {
  def next(token: FQL_TOKEN) : FQL_TOKEN
}

trait FQL_META {}

trait FQL_KEYWORD extends FQL_BUFFER {
  def ready : Boolean =
    (cur == ' ' && ((buf.size == 0) || buf == "()"))
  def next : FQL_TOKEN =
    this.asInstanceOf[FQL_TOKEN]
}


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

class FQL_STREAM extends FQL_TOKEN with FQL_KEYWORD {}
class FQL_OR extends FQL_TOKEN with FQL_KEYWORD{}
class FQL_AND extends FQL_TOKEN with FQL_KEYWORD {}


class FQL_KEY extends FQL_TOKEN {
  def ready =
    ((cur == ' ') || (cur == ')')) && (buf.size > 1)
  def next = this
}

class FQL_OPERATOR extends FQL_TOKEN {
  override def buffer(c: Char, s: String) = ""
  def ready =
    (cur == '=')
  def next =
    if (ready) cur match {
      case '=' => new FQL_OP_EQUALS
    } else this
}

trait FQL_OP_UNARY extends FQL_OPERATOR {
  override def next = this
}

trait FQL_OP_VARARG extends FQL_OPERATOR {
  var args : Int
  override def ready = args == 0
  override def next = if (args == 0) this else
    { args -= 1; new FQL_KEY }
}

class FQL_OP_EQUALS extends FQL_OP_VARARG {
  var args : Int = 1
}

class FQL_OP_META extends FQL_OPERATOR with FQL_META {}
class FQL_OP_EXISTS extends FQL_OP_UNARY { }

class FQL_WHERE(negated: Boolean) extends FQL_TOKEN with FQL_STATEMENT {
  var key : FQL_KEY      = null
  var op  : FQL_OPERATOR = null

  override def buffer(_cur: Char, _buf: String) =
    if ((buf == "(") || (buf == ")"))
      ""
    else
      _buf + _cur

  def ready =
    (key != null) && (op != null) && (buf == ")")

  def next =
    if ((key == null) && (buf == "(")) 
      new FQL_KEY
    else
      this

  def next(t: FQL_TOKEN) = t match {
    case k: FQL_KEY =>
      if (key == null)
        { key = k; new FQL_OP_META }
      else
        { /* is value */ this }
    case o: FQL_OPERATOR =>
      { op = o; this }
  }

}


