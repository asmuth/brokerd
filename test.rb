require "socket"

sock = TCPServer.new(2323)

loop do
  conn = sock.accept
  while l = conn.gets
    puts l.inspect
    break if l == "\r\n"
  end

  conn.puts "HTTP/1.1 200 OK"
  conn.puts "Content-Type: Multipart/mixed; boundary=\"fnord\";"
  conn.puts "\r\n"

  conn.puts "fnord"

  5.times do |i|
    conn.puts "--fnord"
    conn.puts "Content-Type: text/plain; charset=utf8"
    conn.puts "\r\n"
    conn.puts "part #{i}"

    if i == 4
      conn.puts "--fnord--"
    else
      conn.puts "--fnord"
    end
    conn.puts "\r\n"

    sleep 0.5
  end
  conn.close
end
