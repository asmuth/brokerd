package com.paulasmuth.fyrehose

trait FilterStack{
  def push(key: String)(lambda: String => Boolean) : Unit
  def eval(key: String, value: String) : Boolean
  def and() : FilterStack
  def or() : FilterStack
}


class OrFilterStack(lst: List[FilterStack] = List[FilterStack]()) extends FilterStack{

  def push(key: String)(lambda: String => Boolean) : Unit =
    lst.head.push(key)(lambda)


  def and() : FilterStack =
    new OrFilterStack(new AndFilterStack(lst.head) :: lst.tail)


  def or() : FilterStack =
    new OrFilterStack(new AndFilterStack() :: lst)


  def eval(key: String, value: String) = {
    true
  }

}


class AndFilterStack(next: FilterStack = null) extends FilterStack{

  var fkey    : String            = null
  var flambda : String => Boolean = null


  def push(key: String)(lambda: String => Boolean) : Unit = {
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


  def eval(key: String, value: String) = {
    true
  }


}
