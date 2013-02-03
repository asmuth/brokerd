require "rubygems"

$: << ::File.expand_path("../../fyrehose-ruby/lib", __FILE__)
require "fyrehose"

require "ripl"
Ripl.start :binding => binding
