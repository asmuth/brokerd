require "rubygems"
require "eventmachine"

$: << ::File.expand_path("../../fyrehose-ruby/lib", __FILE__)
require "fyrehose"

module FyrehosePOC

  def self.channels
    @@channels ||= Hash.new{ |h,k| h[k] = [] }
  end

  def self.generate_txid
    rand(8**32).to_s(36)
  end

end

class FyrehosePOC::Connection < EventMachine::Connection

  def post_init
    @input_stream = Fyrehose::InputStream.new
  end

  def deliver(msg)
    txid = FyrehosePOC.generate_txid
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
            FyrehosePOC.channels[msg[:channel]] << self
          else
            FyrehosePOC.channels[msg[:channel]].delete(self)
          end

        when :data
          FyrehosePOC.channels[msg[:channel]].each do |recv|
            recv.deliver(msg)
          end
          send_data("##{msg[:txid]} $0\n")

      end
    end
  end

  def unbind
    FyrehosePOC.channels.each do |key, channel|
      channel.delete(self)
    end
  end

end

EventMachine.run do
  EventMachine.open_datagram_socket("0.0.0.0", 2323, FyrehosePOC::Connection)
  EventMachine.start_server("0.0.0.0", 2323, FyrehosePOC::Connection)
end
