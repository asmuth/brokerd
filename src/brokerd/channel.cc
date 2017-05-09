/**
 * This file is part of the "brokerd" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <brokerd/channel.h>
#include <brokerd/util/stringutil.h>

namespace brokerd {

Option<ChannelID> ChannelID::fromString(const std::string& s) {
  if (s.empty() || !StringUtil::isShellSafe(s)) {
    return None<ChannelID>();
  }

  return Some(ChannelID(s));
}

ChannelID::ChannelID(const std::string& id) : id_(id) {}

const std::string& ChannelID::str() const {
  return id_;
}

ReturnCode Channel::createChannel(
    const std::string& path,
    std::shared_ptr<Channel>* channel) {
  ChannelSegment segment;
  segment.offset_begin = 0;
  segment.offset_head = 0;

  channel->reset(new Channel(path, {segment}));
  return ReturnCode::success();
}

Channel::Channel(
    const std::string& path,
    std::list<ChannelSegment> segments /* = {} */) :
    path_(path),
    segments_(segments) {}

ReturnCode Channel::appendMessage(const std::string& message, uint64_t* offset) {
  std::unique_lock<std::mutex> lk(mutex_);

  auto& seg = segments_.back();

  *offset = ++seg.offset_head;

  seg.data.emplace_back(Message {
    .offset = seg.offset_head,
    .next_offset = seg.offset_head + 1,
    .data = message
  });

  return ReturnCode::success();
}

ReturnCode Channel::fetchMessages(
    uint64_t start_offset,
    int batch_size,
    std::list<Message>* entries) {
  std::unique_lock<std::mutex> lk(mutex_);
  *entries = segments_.back().data;
  return ReturnCode::success();
}

} // namespace brokerd

