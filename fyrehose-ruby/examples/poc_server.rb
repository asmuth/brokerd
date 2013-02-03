$: << ::File.expand_path("../../lib", __FILE__)

require "rubygems"
require "eventmachine"
require "fyrehose"

module POCServer

  def self.channels
    @@channels ||= Hash.new{ |h,k| h[k] = [] }
  end

end

class POCServer::Connection < EventMachine::Connection

  def post_init
    @input_stream = Fyrehose::InputStream.new
  end

  def deliver(msg)
    txid = Fyrehose.next_txid
    data = msg[:body]
    channel = msg[:channel]

    send_data("##{txid} @#{channel} *#{data.size} #{data}\n")
  end

  def receive_data(chunk)
    @input_stream << chunk

    @input_stream.each do |msg|
      case msg[:type]

        when :flags
          if msg[:flags] & 1 == 1
            POCServer.channels[msg[:channel]] << self
          else
            POCServer.channels[msg[:channel]].delete(self)
          end

        when :data
          POCServer.channels[msg[:channel]].each do |recv|
            recv.deliver(msg)
          end
          send_data("##{msg[:txid]} $0\n")

      end
    end
  end

  def unbind
    POCServer.channels.each do |key, channel|
      channel.delete(self)
    end
  end

end

EventMachine.run do
  EventMachine.open_datagram_socket("0.0.0.0", 2323, POCServer::Connection)
  EventMachine.start_server("0.0.0.0", 2323, POCServer::Connection)
end
