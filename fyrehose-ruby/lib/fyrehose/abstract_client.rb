class Fyrehose::AbstractClient

  TIMEOUT = 0.1

  def initialize(host, port, opts = {})
    @host = host
    @port = port
    @opts = opts

    @timeout = if opts[:timeout]
      opts[:timeout].to_f
    else
      TIMEOUT
    end
  end

  def deliver(channel, data)
    channel = channel.to_s
    data = data.to_s

    if channel.include?(" ")
      raise Fyrehose::Error.new("channel names must not include whitespace")
    end

    txid = Fyrehose.next_txid
    send_data("##{txid} @#{channel} *#{data.size} #{data}\n")
  end

private

  def send_data(data)
    raise "implement me"
  end

end
