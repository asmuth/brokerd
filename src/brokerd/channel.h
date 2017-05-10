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

const size_t kMaxSegmentSize = (2 << 19) * 512; // 512 MB
const size_t kSegmentHeaderSize = 4096;
const std::array<uint8_t, 4> kMagicBytes = { 0x17, 0xff, 0x23, 0x05 };
const std::array<uint8_t, 4> kVersion = { 0x01, 0x00, 0x00, 0x00 };
const size_t kSegmentHeaderTransactionOffset = 8;

class ChannelID {
public:

  static Option<ChannelID> fromString(const std::string& s);

  const std::string& str() const;

protected:
  ChannelID(const std::string& id);
  std::string id_;
};

struct ChannelSegment {
  uint64_t offset_begin;
  uint64_t offset_head;
  std::list<Message> data;
};

struct ChannelSegmentHandle {
  ChannelSegmentHandle();
  ~ChannelSegmentHandle();
  int fd;
  uint64_t offset_begin;
  uint64_t offset_head;
};

struct ChannelSegmentTransaction {
  uint64_t offset_head;
};

class Channel {
public:

  static ReturnCode createChannel(
      const std::string& path,
      std::shared_ptr<Channel>* channel);

  /**
   * Insert a messsage into the channel and return the offset at which the message
   * was written.
   *
   * @param message the record to append to the topic
   * @return the offset at which the record was written
   */
  ReturnCode appendMessage(const std::string& message, uint64_t* offset);

  /**
   * Read one or more entries from the channel at or after the provided start
   * offset. If the channeloffset references a deleted/expired entry, the next
   * valid entry will be returned. It is always valid to call this method with
   * a start offset of zero to retrieve the first entry or entries from the
   * channel.
   *
   * @param start_offset the start offset to read from
   */
  ReturnCode fetchMessages(
      uint64_t start_offset,
      int batch_size,
      std::list<Message>* entries);

protected:

  Channel(
      const std::string& path,
      std::list<ChannelSegment> segments_archive,
      std::unique_ptr<ChannelSegmentHandle> segment_handle);

  std::mutex mutex_;
  std::string path_;
  std::list<ChannelSegment> segments_archive_;
  std::unique_ptr<ChannelSegmentHandle> segment_handle_;
};

ReturnCode segmentCreate(
    const std::string& channel_path,
    uint64_t start_offset,
    std::unique_ptr<ChannelSegmentHandle>* segment);

void transactionEncode(
    const ChannelSegmentTransaction& tx,
    std::string* buf);

} // namespace brokerd

