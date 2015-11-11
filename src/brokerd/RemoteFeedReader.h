/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#ifndef _FNORD_FEEDS_REMOTEFEEDREADER_H
#define _FNORD_FEEDS_REMOTEFEEDREADER_H
#include "stx/stdtypes.h"
#include "stx/option.h"
#include "stx/rpc/RPC.h"
#include "stx/rpc/RPCClient.h"
#include "brokerd/FeedEntry.h"

namespace stx {
namespace feeds {

class RemoteFeedReader {
public:
  static const size_t kDefaultBatchSize = 1024;
  static const size_t kDefaultMaxBufferSize = 8192;

  RemoteFeedReader(RPCClient* rpc_client);

  Option<FeedEntry> fetchNextEntry();
  void waitForNextEntry();
  void fillBuffers();

  void addSourceFeed(
      URI rpc_url,
      String feed_name,
      uint64_t initial_offset,
      size_t batch_size = kDefaultBatchSize,
      size_t max_buffer_size = kDefaultMaxBufferSize);

  void exportStats(
      const String& path_prefix = "/fnord/feeds/reader/",
      stats::StatsRepository* stats_repo = nullptr);

  /**
   * Return the <low, high> time watermarks
   */
  Pair<UnixTime, UnixTime> watermarks() const;

  /**
   * Return the current <feed_name, offset> stream offsets
   */
  Vector<Pair<String, uint64_t>> streamOffsets() const;

  UnixTime lowWatermark() const;
  UnixTime highWatermark() const;

  void setTimeBackfill(Function<UnixTime (const FeedEntry& entry)> fn);

  void setMaxSpread(Duration duration);

protected:

  class SourceFeed : public RefCounted {
  public:
    URI rpc_url;
    String feed_name;
    unsigned max_buffer_size;
    unsigned batch_size;
    Deque<FeedEntry> read_buffer;
    bool is_fetching;
    uint64_t next_offset;
    uint64_t consumed_offset;
    UnixTime stream_time;
  };

  RPCClient* rpc_client_;
  RefPtrVector<SourceFeed> sources_;

  Wakeup data_available_wakeup_;
  mutable std::mutex mutex_;

  Function<UnixTime (const FeedEntry& entry)> time_backfill_fn_;
  Duration max_spread_;

  //stx::stats::Counter<uint64_t> stat_entries_written_total_;
  //stx::stats::Counter<uint64_t> stat_entries_written_success_;
  //stx::stats::Counter<uint64_t> stat_entries_written_error_;
  //stx::stats::Counter<uint64_t> stat_entries_written_retry_;
};

}
}
#endif
