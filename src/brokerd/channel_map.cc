/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <brokerd/util/logging.h>
#include <brokerd/util/fileutil.h>
#include <brokerd/channel_map.h>

namespace brokerd {

ReturnCode ChannelMap::openDirectory(
    const std::string& data_dir,
    std::unique_ptr<ChannelMap>* channel_map) try {
  if (!FileUtil::isDirectory(data_dir)) {
    return ReturnCode::errorf("EARG", "not a directory: $0", data_dir);
  }

  FileLock data_dir_lock(FileUtil::joinPaths(data_dir, "~lock"));
  data_dir_lock.lock();

  return ReturnCode::success();
} catch (const std::exception& e) {
  return ReturnCode::error(e);
}

ChannelMap::ChannelMap(
    const String& data_dir,
    FileLock&& data_dir_lock) :
    data_dir_(data_dir),
    data_dir_lock_(std::move(data_dir_lock_)) {}

std::string ChannelMap::getHostID() const noexcept {
  return hostid_;
}

//FeedService::FeedService(const String& data_dir) :
//    lock_(FileUtil::joinPaths(data_dir, "index.lck")) {
//  lock_.lock(false);
//
//  auto hostid_file = FileUtil::joinPaths(data_dir, "hostid.idx");
//  if (!FileUtil::exists(hostid_file)) {
//    hostid_ = rnd_.hex128();
//
//    {
//      auto f = File::openFile(hostid_file + "~", File::O_CREATE | File::O_WRITE);
//      f.write(hostid_.data(), hostid_.size());
//    }
//
//    FileUtil::mv(hostid_file + "~", hostid_file);
//  } else {
//    hostid_ = FileUtil::read(hostid_file).toString();
//  }
//
//  //file_repo_.listFiles([this] (const std::string& filename) -> bool {
//  //  if (StringUtil::endsWith(filename, ".idx") ||
//  //      StringUtil::endsWith(filename, ".lck")) {
//  //    return true;
//  //  }
//
//  //  reopenTable(filename);
//  //  return true;
//  //});
//}
//
//uint64_t FeedService::append(std::string stream_key, std::string entry) {
//  auto stream = openStream(stream_key, true);
//  return stream->append(entry);
//}
//
//uint64_t FeedService::insert(const String& topic, const Buffer& record) {
//  auto stream = openStream(topic, true);
//  return stream->append(record);
//}
//
//ReturnCode FeedService::fetch(
//      std::string stream_key,
//      uint64_t offset,
//      int batch_size,
//      std::list<FeedEntry>* entries) {
//  auto stream = openStream(stream_key, false);
//  stream->fetch(offset, batch_size, entries);
//  return ReturnCode::success();
//}
//
//LogStream* FeedService::openStream(const std::string& name, bool create) {
//  std::unique_lock<std::mutex> l(streams_mutex_);
//
//  LogStream* stream = nullptr;
//
//  auto stream_iter = streams_.find(name);
//  if (stream_iter == streams_.end()) {
//    if (!create) {
//      RAISEF(kIndexError, "no such stream: $0", name);
//    }
//
//    stream = new LogStream(name, this);
//    streams_.emplace(std::make_pair(name, std::unique_ptr<LogStream>(stream)));
//  } else {
//    stream = stream_iter->second.get();
//  }
//
//  return stream;
//}
//
////void FeedService::reopenTable(const std::string& file_path) {
////  stx::sstable::SSTableRepair repair(file_path);
////  if (!repair.checkAndRepair(true)) {
////    RAISEF(kRuntimeError, "corrupt sstable: $0", file_path);
////  }
////
////  auto file = File::openFile(file_path, File::O_READ);
////  sstable::SSTableReader reader(std::move(file));
////
////  LogStream::TableHeader table_header;
////
////  try {
////    table_header = stx::json::fromJSON<LogStream::TableHeader>(
////        reader.readHeader());
////  } catch (const Exception& e) {
////    stx::logError(
////        "fnord.feed",
////        e,
////        "error while reading table header of file: $0",
////        file_path);
////
////    throw e;
////  }
////
////  if (reader.bodySize() == 0) {
////    auto writer = sstable::SSTableEditor::reopen(
////        file_path,
////        sstable::IndexProvider{});
////    writer->finalize();
////  }
////
////  auto stream = openStream(table_header.stream_name, true);
////  stream->reopenTable(file_path);
////}
//
//String FeedService::hostID() {
//  return hostid_;
//}

} // namespace brokerd

