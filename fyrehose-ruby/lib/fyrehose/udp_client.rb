class Fyrehose::UDPClient < Fyrehose::AbstractClient

  def open_connection
    @sock = UDPSocket.new
    @sock.connect(@host, @port)
  end

  def send_data(data)
    Timeout::timeout(@timeout) do
      open_connection unless @sock
      @sock.send(data, 0); 0
    end
  rescue Exception => e
    @sock = nil
    raise e
  end

end
