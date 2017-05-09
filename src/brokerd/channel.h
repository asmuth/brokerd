/**
 * This file is part of the "brokerd" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#pragma once
#include <brokerd/util/return_code.h>
#include <brokerd/util/option.h>
#include <brokerd/message.h>

namespace brokerd {

class ChannelID {
public:

  static Option<ChannelID> fromString(const std::string& s);

  const std::string& str() const;

protected:
  ChannelID(const std::string& id);
  std::string id_;
};

class Channel {
public:

  static const size_t kMaxSegmentSize = (2 << 19) * 512; // 512 MB
  static const size_t kSegmentHeaderSize = 4096;

  struct ChannelSegment {
    uint64_t offset_begin;
    uint64_t offset_head;
  };

  Channel(
      const std::string& path,
      std::list<ChannelSegment> segments = {});

  ReturnCode append(const std::string& entry);

  ReturnCode fetch(
      uint64_t offset,
      int batch_size,
      std::list<Message>* entries);

protected:
  std::string path_;
  std::list<ChannelSegment> segments_;
};

} // namespace brokerd

