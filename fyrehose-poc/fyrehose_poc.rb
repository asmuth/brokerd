require "rubygems"
require "eventmachine"

module Fyrehose

  class InputStream

    attr_accessor :state, :buf, :pos

    def initialize
      @state = -7
      @pos = 0
      @buf = ""
    end

    def <<(chunk)
      buf << chunk
    end

    def each
      trim = 0

      while pos < buf.size do
        case state

          when -7
            self.state += 1
            type = nil
            txid = ""
            channel = ""
            len_or_flags = ""
            body = ""
            next

          when -6
            raise "protocol error" if buf[pos] != "#"
            self.state += 1

          when -5
            if buf[pos] == " "
              self.state += 1
            else
              txid << buf[pos]
            end

          when -4
            if buf[pos] == "$"
              type = :ack
              self.state = -1
            elsif buf[pos] == "@"
              self.state += 1
            else
              raise "protocol error"
            end

          when -3
            if buf[pos] == " "
              self.state += 1
            else
              channel << buf[pos]
            end

          when -2
            self.state += 1
            if buf[pos] == "*"
              type = :data
            elsif buf[pos] == "+"
              type = :flags
            else
              raise "protocol error #{buf[pos]}"
            end

          when -1
            if buf[pos] == " " || buf[pos] == "\n"
              len_or_flags = len_or_flags.to_i
              if type == :data
                self.state = len_or_flags
              else
                self.state += 1
                next
              end
            else
              len_or_flags << buf[pos]
            end

          when 0
            yield({
              :type => type,
              :txid => txid,
              :channel => channel,
              :flags => len_or_flags,
              :body => body
            })
            trim = pos + 1
            self.state = -7

          else
            self.state -= 1
            body << buf[pos]

        end

        self.pos += 1
      end

      if trim > 0
        self.pos -= trim
        self.buf = self.buf[trim..-1]
      end
    end

  end

end

class FyrehosePOC < EventMachine::Connection

  def post_init
    @input_stream = Fyrehose::InputStream.new
    puts "connect"
  end

  def receive_data(chunk)
    @input_stream << chunk

    @input_stream.each do |msg|
      puts msg.inspect
    end
  end

end

EventMachine.run do
  EventMachine.start_server("0.0.0.0", 2323, FyrehosePOC)
end
