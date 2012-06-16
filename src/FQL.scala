package com.paulasmuth.fyrehose;

// todo: since(), until(), less/greater-than, regex, include, exists, boolean, time, except, only
trait FQL_VAL {}
trait FQL_META {}
trait FQL_OP {}
trait FQL_HEAD {}

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

class FQL_ATOM extends FQL_TOKEN with FQL_META {
  def ready =
    ((cur == ' ') || (cur == '(') || (cur == ')') ||
    (cur == '=') || (cur == '.')) && (buf.size > 0)
  def next =
    if (ready unary_!)
      this
    else buf.trim match {
      case "stream"    => new FQL_STREAM
      case "and"       => new FQL_AND
      case "or"        => new FQL_OR
      case "where"     => new FQL_WHERE(true)
      case "where_not" => new FQL_WHERE(false)
      case "includes"  => new FQL_OPERATOR_INCLUDES
      case "exists"    => new FQL_OPERATOR_EXISTS
      case "true"      => new FQL_TRUE
      case "false"     => new FQL_FALSE
      case _           => new FQL_KEY(buf)
    }
}

class FQL_OPERATOR extends FQL_TOKEN with FQL_OP with FQL_META {
  def ready = next != this
  def next = buf.trim match {
    case "=" => new FQL_OPERATOR_EQUALS
    case ""  => this
    case _   => new FQL_ATOM
  }
}

class FQL_VALUE extends FQL_TOKEN with FQL_VAL with FQL_META {
  def ready = next != this
  def next = cur match {
    case ' '  => this
    case '\'' => new FQL_STRING
    case '"'  => new FQL_STRING
    case '`'  => new FQL_STRING
    case '0'  => new FQL_NUMBER
    case '1'  => new FQL_NUMBER
    case '2'  => new FQL_NUMBER
    case '3'  => new FQL_NUMBER
    case '4'  => new FQL_NUMBER
    case '5'  => new FQL_NUMBER
    case '6'  => new FQL_NUMBER
    case '7'  => new FQL_NUMBER
    case '8'  => new FQL_NUMBER
    case '9'  => new FQL_NUMBER
    case _    => new FQL_ATOM
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
class FQL_OPERATOR_INCLUDES extends FQL_OPERATOR_BINARY {}
class FQL_OPERATOR_EXISTS extends FQL_OPERATOR_UNARY {}


trait FQL_BOOL extends FQL_TOKEN with FQL_VAL {
  def get : Boolean
  def next = this
  def ready = true
}

class FQL_TRUE extends FQL_BOOL {
  def get = true
}
class FQL_FALSE extends FQL_BOOL {
  def get = false
}

class FQL_NUMBER extends FQL_TOKEN with FQL_META {

  def ready =
    ((cur < 48) || (cur > 57)) &&
    ((cur != '.') || (buf.indexOf('.') != -1))

  def next =
    if (ready unary_!) this
    else if (buf.indexOf('.') != -1) new FQL_FLOAT(buf)
    else new FQL_INTEGER(buf)

}

class FQL_INTEGER(_buf: String) extends FQL_TOKEN with FQL_VAL {
  def ready = true
  def next = this
  def get = _buf.toInt
}

class FQL_FLOAT(_buf: String) extends FQL_TOKEN with FQL_VAL {
  def ready = true
  def next = this
  def get = _buf.toDouble
}

class FQL_STRING extends FQL_TOKEN with FQL_VAL {
  var quot : Char = 0

  override def buffer(_cur: Char, _buf: String) =
    if (ready) _buf else (_buf + _cur)

  def ready =
    (buf.size > 1) &&
    (buf(buf.size - 1) == quot) &&
    (buf(buf.size - 2) != '\\')

  def next =
    if (quot == 0) { quot = cur; this }
    else this

  def get =
    buf.substring(1, buf.size - 1)

}

class FQL_KEY(_buf: String = "") extends FQL_TOKEN with FQL_VAL {
  def ready =
    (cur == ' ') || (cur == ')') || (cur == '=')
  def next = this
  def get = _buf
}

class FQL_WHERE(negated: Boolean) extends FQL_TOKEN with FQL_STATEMENT {
  var left  : FQL_VAL  = null
  var right : FQL_VAL  = null
  var op    : FQL_OP   = null

  override def buffer(_cur: Char, _buf: String) =
    if ((buf == "(") || (buf == ")"))
      ""
    else
      (_buf + _cur).trim

  def ready =
    (left != null) &&
    (op != null) &&
    (right != null) &&
    (buf == ")")

  def next =
    if ((left == null) && (buf == "("))
      new FQL_VALUE
    else
      this

  def next(t: FQL_TOKEN) = t match {
    case v: FQL_VAL =>
      if (left == null)
        { left = v; new FQL_OPERATOR }
      else
        { right = v; this }
    case o: FQL_OP =>
      { op = o; this }
  }

}


