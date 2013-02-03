require "socket"
require "timeout"

require "fyrehose/errors"
require "fyrehose/input_stream"
require "fyrehose/abstract_client"
require "fyrehose/tcp_client"
require "fyrehose/udp_client"

module Fyrehose

  def self.next_txid
    rand(8**32).to_s(36)
  end

end
