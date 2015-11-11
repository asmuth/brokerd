/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#ifndef _FNORD_FEEDS_REMOTEFEEDWRITER_H
#define _FNORD_FEEDS_REMOTEFEEDWRITER_H
#include "stx/stdtypes.h"
#include "stx/option.h"
#include "stx/rpc/RPC.h"
#include "stx/rpc/RPCClient.h"
#include "brokerd/FeedEntry.h"

namespace stx {
namespace feeds {

class RemoteFeedWriter : public RefCounted {
public:
  static const int kDefaultMaxBufferSize = 8192;

  RemoteFeedWriter(
      RPCClient* rpc_client,
      size_t max_buffer_size = kDefaultMaxBufferSize);

  void appendEntry(const String& entry_data);

  void addTargetFeed(
      URI url,
      String feed_name,
      unsigned max_concurrent_request);

  void exportStats(
      const String& path_prefix = "/fnord/feeds/writer/",
      stats::StatsRepository* stats_repo = nullptr);

  size_t queueLength() const;
  size_t maxQueueLength() const;

protected:

  class TargetFeed : public RefCounted {
  public:
    URI rpc_url;
    String feed_name;
    unsigned max_concurrent_requests;
    std::atomic<unsigned> cur_requests;
  };

  void flushBuffer();
  void flushBuffer(RefPtr<TargetFeed> feed);

  stx::RPCClient* rpc_client_;
  size_t max_buffer_size_;

  std::mutex write_queue_mutex_;
  Deque<String> write_queue_;

  std::mutex target_feeds_mutex_;
  Vector<RefPtr<TargetFeed>> target_feeds_;
  size_t sequence_;

  stx::stats::Counter<uint64_t> stat_entries_written_total_;
  stx::stats::Counter<uint64_t> stat_entries_written_success_;
  stx::stats::Counter<uint64_t> stat_entries_written_error_;
  stx::stats::Counter<uint64_t> stat_entries_written_retry_;

  std::atomic<size_t> queue_length_;
};

}
}
#endif
