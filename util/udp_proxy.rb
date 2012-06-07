require "socket"

rx = UDPSocket.new
rx.bind "0.0.0.0", 2323

tx = TCPSocket.new("localhost", 2323)

while chunk = rx.recvfrom(256)
  tx.write chunk.first
end

