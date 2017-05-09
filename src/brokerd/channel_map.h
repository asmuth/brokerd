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

  /**
   * Open all channel files in a directory and aquire a lockfile
   */
  static ReturnCode openDirectory(
      const std::string& data_dir,
      std::unique_ptr<ChannelMap>* channel_map);

  /**
   * Find and possibly create a channel
   */
  ReturnCode findChannel(
      const ChannelID& channel_id,
      bool create,
      std::shared_ptr<Channel>* channel);

  /**
   * Return the unique server id
   */
  std::string getHostID() const noexcept;

protected:

  ChannelMap(
      const String& data_dir,
      FileLock&& data_dir_lock);

  std::string data_dir_;
  FileLock data_dir_lock_;
  std::mutex channels_mutex_;
  std::map<std::string, std::shared_ptr<Channel>> channels_;
  std::string hostid_;
  Random rnd_;
};

} // brokerd

