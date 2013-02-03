$: << ::File.expand_path("../../lib", __FILE__)

require "rubygems"
require "fyrehose"
require "fyrehose/reactor"

Fyrehose::Reactor.run("localhost", 2323) do

  subscribe :fnord

  on_message do |channel, data|
    puts "#{channel} => #{data}"
  end

end
