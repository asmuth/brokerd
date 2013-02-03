module Fyrehose
  class ProtocolError < StandardError

    INFO_SIZE = 30

    def initialize(buf=nil, pos=nil)
      return unless buf
      offset = [0, pos - (INFO_SIZE / 2)].max
      @info = "\n    => "
      @info << buf[offset..offset + INFO_SIZE].gsub("\n", " ")
      @info << "\n#{" " * (pos - offset + 7)}^\n"
    end

    def to_s
      "invalid token#{@info}"
    end

  end
end
