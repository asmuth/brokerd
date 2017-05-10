/**
 * This file is part of the "brokerd" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <assert.h>
#include <unistd.h>
#include <cstring>
#include <brokerd/channel.h>
#include <brokerd/util/stringutil.h>
#include <brokerd/util/file.h>
#include <brokerd/util/varint.h>
#include <brokerd/util/fileutil.h>

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

ChannelSegmentHandle::ChannelSegmentHandle() : fd(-1) {}

ChannelSegmentHandle::~ChannelSegmentHandle() {
  if (fd >= 0) {
    ::close(fd);
  }
}

ReturnCode Channel::createChannel(
    const std::string& path,
    std::shared_ptr<Channel>* channel) {
  std::unique_ptr<ChannelSegmentHandle> segment;
  auto rc = segmentCreate(path, 0, &segment);
  if (!rc.isSuccess()) {
    return rc;
  }

  channel->reset(new Channel(path, {}, std::move(segment)));
  return ReturnCode::success();
}

Channel::Channel(
    const std::string& path,
    std::list<ChannelSegment> segments_archive,
    std::unique_ptr<ChannelSegmentHandle> segment_handle) :
    path_(path),
    segments_archive_(segments_archive),
    segment_handle_(std::move(segment_handle)) {}

ReturnCode Channel::appendMessage(const std::string& message, uint64_t* offset) {
  std::unique_lock<std::mutex> lk(mutex_);

  *offset = segment_handle_->offset_head;

  auto rc = segmentAppend(segment_handle_.get(), message.data(), message.size());
  if (!rc.isSuccess()) {
    return rc;
  }

  return segmentCommit(segment_handle_.get());
}

ReturnCode Channel::fetchMessages(
    uint64_t start_offset,
    int batch_size,
    std::list<Message>* entries) {
  std::unique_lock<std::mutex> lk(mutex_);
  //*entries = segments_.back().data;
  return ReturnCode::success();
}

ReturnCode segmentCreate(
    const std::string& channel_path,
    uint64_t start_offset,
    std::unique_ptr<ChannelSegmentHandle>* segment) {
  auto segment_path = StringUtil::format("$0~$1", channel_path, start_offset); 
  auto segment_file = File::openFile(
    segment_path + "~",
    File::O_READ | File::O_WRITE | File::O_CREATEOROPEN | File::O_TRUNCATE,
    0644);

  ChannelSegmentTransaction tx;
  tx.offset_head = start_offset;

  std::string tx_buf;
  transactionEncode(tx, &tx_buf);

  std::string segment_header;
  segment_header.append((const char*) kMagicBytes.data(), kMagicBytes.size());
  segment_header.append((const char*) kVersion.data(), kVersion.size());
  segment_header.append(tx_buf);

  assert(segment_header.size() <= kSegmentHeaderSize);

  if (segment_header.size() < kSegmentHeaderSize) {
    segment_header.append(std::string(kSegmentHeaderSize - segment_header.size(), 0));
  }

  {
    auto rc = ::write(
        segment_file.fd(),
        segment_header.data(),
        segment_header.size());

    if (rc < 0 || rc != segment_header.size()) {
      return ReturnCode::errorf(
          "EIO",
          "can't write segment header to '$0': $1",
          segment_path,
          strerror(errno));
    }
  }

  FileUtil::mv(segment_path + "~", segment_path);

  std::unique_ptr<ChannelSegmentHandle> s(new ChannelSegmentHandle());
  s->offset_begin = start_offset;
  s->offset_head = start_offset;
  s->fd = segment_file.releaseFD();

  *segment = std::move(s);
  return ReturnCode::success();
}

ReturnCode segmentAppend(
    ChannelSegmentHandle* segment,
    const char* message,
    size_t message_len) {

  size_t message_envelope_size;
  auto rc = messageWrite(
      message,
      message_len,
      segment->fd,
      &message_envelope_size);

  if (!rc.isSuccess()) {
    return rc;
  }

  segment->offset_head += message_envelope_size;
  return ReturnCode::success();
}

ReturnCode segmentCommit(ChannelSegmentHandle* segment) {
  ChannelSegmentTransaction tx;
  tx.offset_head = segment->offset_head;

  std::string tx_buf;
  transactionEncode(tx, &tx_buf);

  if (::fdatasync(segment->fd) == -1) {
    return ReturnCode::errorf("EIO", "fsync() failed: $0", strerror(errno));
  }

  int rc = ::pwrite(
      segment->fd,
      tx_buf.data(),
      tx_buf.size(),
      kSegmentHeaderTransactionOffset);

  if (rc < 0 || rc != tx_buf.size()) {
    return ReturnCode::errorf("EIO", "write() failed: $0", strerror(errno));
  }

  return ReturnCode::success();
}

void transactionEncode(
    const ChannelSegmentTransaction& tx,
    std::string* buf) {
  std::string b(sizeof(tx.offset_head), 0);
  memcpy(&b[0], &tx.offset_head, sizeof(tx.offset_head));
  buf->append(b);
}

} // namespace brokerd

