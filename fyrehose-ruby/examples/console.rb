$: << ::File.expand_path("../../lib", __FILE__)

require "rubygems"
require "fyrehose"
require "ripl"

Ripl.start :binding => binding
