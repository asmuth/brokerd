require "rubygems"
require "eventmachine"

$: << ::File.expand_path("../../fyrehose-ruby/lib", __FILE__)
require "fyrehose"

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
