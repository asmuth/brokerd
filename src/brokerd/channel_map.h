/**
 * This file is part of the "brokerd" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#pragma once
#include <mutex>
#include <stdlib.h>
#include <set>
#include <string>
#include <unordered_map>
#include <brokerd/util/file_lock.h>
#include <brokerd/util/random.h>
#include <brokerd/channel.h>
#include <brokerd/message.h>

namespace brokerd {

class ChannelMap {
public:

  ChannelMap(const String& data_dir);

  /**
   * Insert a record into the topic referenced by `topic` and return the offset
   * at which the record was written. Will create a new topic if the referenced
   * topic does not exist yet.
   *
   * @param topic the name/key of the topic
   * @param record the record to append to the topic
   * @return the offset at which the record was written
   */
  ReturnCode appendMessage(
      const std::string& channel,
      const std::string& message,
      uint64_t* offset);

  /**
   * Read one or more entries from the stream at or after the provided start
   * offset. If the start offset references a deleted/expired entry, the next
   * valid entry will be returned. It is always valid to call this method with
   * a start offset of zero to retrieve the first entry or entries from the
   * stream.
   *
   * @param start_offset the start offset to read from
   */
  ReturnCode fetchMessages(
      const std::string& channel,
      uint64_t offset,
      uint32_t batch_size,
      std::list<Message>* entries);

  std::string getHostID();

protected:
  std::mutex channels_mutex_;
  std::map<std::string, std::unique_ptr<Channel>> channels_;
  FileLock lock_;
  std::string hostid_;
  Random rnd_;
};

} // brokerd

