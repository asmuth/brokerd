class Fyrehose::TCPClient < Fyrehose::AbstractClient

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
