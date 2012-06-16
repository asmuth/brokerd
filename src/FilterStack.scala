package com.paulasmuth.fyrehose

trait FilterStack{
  def push(key: FQL_KEY)(lambda: Event => Boolean) : Unit
  def eval(event: Event) : Boolean
  def and() : FilterStack
  def or() : FilterStack
}


class OrFilterStack(lst: List[FilterStack] = List[FilterStack]()) extends FilterStack{

  def push(key: FQL_KEY)(lambda: Event => Boolean) : Unit =
    lst.head.push(key)(lambda)


  def and() : FilterStack =
    new OrFilterStack(new AndFilterStack(lst.head) :: lst.tail)


  def or() : FilterStack =
    new OrFilterStack(new AndFilterStack() :: lst)


  def eval(event: Event) : Boolean =
    lst.find(filter => filter.eval(event)).isEmpty unary_!

}


class AndFilterStack(next: FilterStack = null) extends FilterStack{

  var fkey    : FQL_KEY          = null
  var flambda : Event => Boolean = null


  def push(key: FQL_KEY)(lambda: Event => Boolean) : Unit = {
    if (fkey != null)
      throw new ParseException("invalid filter chain")

    fkey    = key
    flambda = lambda
  }


  def and() : FilterStack =
    if (fkey == null)
      throw new ParseException("invalid filter chain")
    else
      new AndFilterStack(this)


  def or() : FilterStack =
    if (fkey == null)
      throw new ParseException("invalid filter chain")
    else
      new OrFilterStack(List(new AndFilterStack(), this))


  def eval(event: Event) : Boolean = try {

    if ((fkey != null) && (event.exists(fkey) unary_!))
      return false

    else if ((fkey != null) && (flambda(event) unary_!))
      return false

    else if (next == null)
      return true

    else
      return next.eval(event)

  } catch {
    case e: RuntimeException => return false
  }

}
