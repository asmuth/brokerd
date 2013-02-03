class Fyrehose::TCPClient

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
    Timeout::timeout(@timeout) do
      @sock = TCPSocket.new(@host, @port) unless @sock
      @sock.send(data, 0)
      parse_response(@sock.gets)
    end
  rescue Exception => e
    @sock = nil
    raise e
  end

  def parse_response(str)
    raise Fyrehose::ConnectionError.new unless str
    m = str.match(/#[^ ]+ \$([0-9]+)\n/)
    raise Fyrehose::ProtocolError.new unless m
    m[1].to_i
  end

end
