package com.paulasmuth.fyrehose

trait FilterStackChainMode{}
object FilterStackAndChain extends FilterStackChainMode
object FilterStackOrChain extends FilterStackChainMode

class FilterStack(chain_mode: FilterStackChainMode, lst: List[FilterStack] = List[FilterStack]()){

  var fkey    : String            = null
  var flambda : String => Boolean = null

  def push(key: String)(lambda: String => Boolean) : Unit = {
    if (fkey != null)
      throw new ParseException("invalid filter chain")

    fkey    = key
    flambda = lambda
  }

  def and() : FilterStack = {
    if (fkey == null)
      throw new ParseException("invalid filter chain")

    this
  }

  def or() : FilterStack = {
    /*if (filter == null)
      throw new ParseException("invalid filter chain")*/
    this
  }

}
