#pragma once
#include <string>
#include <optional>

struct FileState {
  // The sequence number of the file
  uint64_t seq;

  // The size of the file in bytes
  size_t size;

  // The file descriptor for the file
  int fd;
};

struct PoolState {
  // The directory in which files are stored
  std::string path;

  // The currently active file
  std::optional<FileState> file;

  // The (lowest, highest) sequence number of all existing files
  uint64_t file_list[2];

  // The maximum file size in bytes
  size_t file_size_limit;

  // The maximum number of files to retain
  size_t file_count_limit;
};

PoolState pool_init(
  const std::string& path
);

bool pool_write(
  PoolState* pool,
  const uint8_t* data,
  size_t data_len
);
