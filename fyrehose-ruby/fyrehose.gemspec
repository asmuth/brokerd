# -*- encoding: utf-8 -*-
$:.push File.expand_path("../lib", __FILE__)

Gem::Specification.new do |s|
  s.name        = "fyrehose"
  s.version     = "0.0.2"
  s.date        = Date.today.to_s
  s.platform    = Gem::Platform::RUBY
  s.authors     = ["Paul Asmuth"]
  s.email       = ["paul@paulasmuth.com"]
  s.homepage    = "http://github.com/paulasmuth/fyrehose"
  s.summary     = %q{ruby client for the fyrehose pub/sub protocol}
  s.description = %q{ruby client for the fyrehose pub/sub protocol}  

  s.licenses    = ["MIT"]

  s.files         = `git ls-files`.split("\n") - [".gitignore", ".rspec", ".travis.yml"]
  s.test_files    = `git ls-files -- spec/*`.split("\n")
  s.require_paths = ["lib"]
end
