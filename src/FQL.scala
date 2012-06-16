package com.paulasmuth.fyrehose;

trait FQL_TOKEN {
  def ready : Boolean
  def next : FQL_TOKEN
  var cur : Char = 0
  var buf : String = null
  def buffer(_cur: Char, _buf: String) : String =
    if (ready) "" else _buf + _cur
  def next(_cur: Char, _buf: String) : FQL_TOKEN =
    { cur=_cur; buf = _buf; next }
}

trait FQL_STATEMENT {
  def next(token: FQL_TOKEN) : FQL_TOKEN
}

class FQL_ATOM extends FQL_TOKEN {
  def ready =
    (cur == ' ') || (cur == '(')
  def next =
    if ((cur != ' ') && (cur != '('))
      this
    else buf match {
      case "stream"    => new FQL_STREAM
      case "where"     => new FQL_WHERE(true)
      case "where_not" => new FQL_WHERE(false)
    }
}

class FQL_STREAM extends FQL_TOKEN {
  def ready = true
  def next = this
}

class FQL_KEY extends FQL_TOKEN {
  def ready =
    (cur == ' ') || (cur == ')')
  def next = this
}

class FQL_OPERATOR extends FQL_TOKEN {
  def ready = cur == ' '
  def next = this
}

class FQL_WHERE(negated: Boolean) extends FQL_TOKEN with FQL_STATEMENT {
  var key : FQL_KEY      = null
  var op  : FQL_OPERATOR = null

  override def buffer(_cur: Char, _buf: String) = ""

  def ready =
    (key != null) && (op != null)

  def next =
    new FQL_KEY

  def next(t: FQL_TOKEN) = t match {
    case k: FQL_KEY =>
      { key = k; new FQL_OPERATOR }
    case o: FQL_OPERATOR =>
      { op = o; this }
  }

}


