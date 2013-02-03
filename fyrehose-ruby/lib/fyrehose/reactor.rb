require "eventmachine"

class Fyrehose::Reactor < EventMachine::Connection

  def self.run(host, port, opts = {}, &block)
    unless block
      raise "missing proc{ |channel,data| } for #run"
    end

    EventMachine.run do
      reactor = EventMachine.connect(host, port, Fyrehose::Reactor)
      reactor.instance_eval(&block)
    end
  end

  def post_init
    @input_stream = Fyrehose::InputStream.new
    @callbacks = []
  end

  def deliver(channel, data)
    send_data("##{next_txid} @#{channel} *#{data.size} #{data}\n")
  end

  def set_flags(channel, flags)
    send_data("##{next_txid} @#{channel} +#{flags}\n")
  end

  def subscribe(channel)
    set_flags(channel, 1)
  end

  def unsubscribe(channel)
    set_flags(channel, 0)
  end

  def on_message(&block)
    @callbacks << block
  end

  def receive_data(chunk)
    @input_stream << chunk

    @input_stream.each do |msg|
      next unless msg[:type] == :data

      @callbacks.each do |block|
        block.call(msg[:channel], msg[:body])
      end
    end
  end

private

  def next_txid
    rand(8**32).to_s(36)
  end

end
