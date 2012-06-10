package com.paulasmuth.fyrehose

import scala.actors._

class StreamQuery(raw: String) extends Query{

  val X_VALIDATE = """^(([a-z]+\([^\)]*\)|and|or) *)+$"""
  val X_EXTRACT  = """(([a-z]+)\(([^\)]*)\)|and|or)"""

  var recv : Actor = null
  var fstack : FilterStack = new AndFilterStack


  if (raw.matches(X_VALIDATE) unary_!)
    throw new ParseException("invalid query: " + raw)

  val xparse = java.util.regex.Pattern
    .compile(X_EXTRACT)
    .matcher(raw)

  while(xparse.find())
    eval(raw.substring(xparse.start, xparse.end))


  def execute(endpoint: Actor) =
    recv = endpoint


  def data(event: Event) =
    if (recv == null){
      println("reschedule query event")
      this ! event
    } else {
      println("query outbound stream sent")
      recv ! new QueryResponseChunk(event.bytes)
      recv ! new QueryResponseChunk("\n".getBytes)
    }


  private def eval(part: String) = {
    println("parsing: " + part)
    //arg.gsub!("\\'", "\x7") # FIXPAUL: hack! ;)

    val x_stream = """^stream\(\)$""".r

    val x_or =  """^or$""".r
    val x_and = """^and$""".r

    val x_where = """^where\(([^ ]+)"""
    val x_where_equals_str = (x_where + """ *= *'([^']*)'\)$""").r

    part match {

      case x_stream() =>
        ()

      case x_or() =>
        fstack = fstack.or()

      case x_and() =>
        fstack = fstack.and()

      case x_where_equals_str(k: String, v: String) => 
        fstack.push(k)((x: String) => x == v )

      case _ =>
        throw new ParseException("invalid query part: " + part)

    }

  }

    //    key_clean = lambda{ |s| 
    //      s.gsub("\x7", "'")
    //       .gsub(/([^\\])\./){ |m| m[0] + "\x06" }
    //       .gsub('\\.', ".") 
    //    }


    /*
      @filters << [key_clean[m[1]], :equals, m[2]]
    elsif m = arg.match(/^#{key_regex} *= *([0-9]+)$/)
      @filters << [key_clean[m[1]], :equals, m[2].to_i]
    elsif m = arg.match(/^#{key_regex} *= *([0-9]+\.[0-9]+)$/)
      @filters << [key_clean[m[1]], :equals, m[2].to_f]
    elsif m = arg.match(/^#{key_regex} *! *'([^']*)'$/)
      @filters << [key_clean[m[1]], :not_equals, m[2]]
    elsif m = arg.match(/^#{key_regex} *! *([0-9]+)$/)
      @filters << [key_clean[m[1]], :not_equals, m[2].to_i]
    elsif m = arg.match(/^#{key_regex} *! *([0-9]+\.[0-9]+)$/)
      @filters << [key_clean[m[1]], :not_equals, m[2].to_f]
    elsif m = arg.match(/^#{key_regex} *< *([0-9]+)$/)
      @filters << [key_clean[m[1]], :less_than, m[2].to_i]
    elsif m = arg.match(/^#{key_regex} *< *([0-9]+\.[0-9]+)$/)
      @filters << [key_clean[m[1]], :less_than, m[2].to_f]
    elsif m = arg.match(/^#{key_regex} *> *([0-9]+)$/)
      @filters << [key_clean[m[1]], :greater_than, m[2].to_i]
    elsif m = arg.match(/^#{key_regex} *> *([0-9]+\.[0-9]+)$/)
      @filters << [key_clean[m[1]], :greater_than, m[2].to_f]
    elsif m = arg.match(/^#{key_regex} *~ *([0-9]+)-([0-9]+)$/)
      @filters << [key_clean[m[1]], :range_include, (m[2].to_i..m[3].to_i)]
    elsif m = arg.match(/^#{key_regex} *~ *([0-9]+\.[0-9]+)-([0-9]+\.[0-9]+)$/)
      @filters << [key_clean[m[1]], :range_include, (m[2].to_f..m[3].to_f)]
    elsif m = arg.match(/^#{key_regex} *& *(([0-9]+),)+([0-9]+)$/)
      @filters << [key_clean[m[1]], :list_include, m[2..-1].map(&:to_i)]
    elsif m = arg.match(/^#{key_regex} *& *(([0-9]+\.[0-9]+),)+([0-9]+\.[0-9]+)$/)
      @filters << [key_clean[m[1]], :list_include, m[2..-1].map(&:to_f)]
    elsif m = arg.match(/^#{key_regex} *& *('[^']*',)+'[^']*'$/)
      @filters << [key_clean[m[1]], :list_include, arg
        .match(/^#{key_regex} *& *(.*)/)[2].scan(/'([^']*)',?/).map do |x|
          x.first.gsub("\x7", "'")
      end.to_a]
    elsif m = arg.match(/^#{key_regex}$/)
      @filters << [key_clean[m[1]], :exists, nil]
    else
      raise InvalidQueryError.new("invalid filter: filter(#{arg})")
    end
    */



}
