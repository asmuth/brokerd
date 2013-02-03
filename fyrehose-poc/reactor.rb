require "rubygems"

$: << ::File.expand_path("../../fyrehose-ruby/lib", __FILE__)
require "fyrehose"
require "fyrehose/reactor"

Fyrehose::Reactor.run("localhost", 2323) do

  subscribe :fnord

  on_message do |channel, data|
    puts "#{channel} => #{data}"
  end

end
