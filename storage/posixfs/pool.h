#pragma once
#include <string>

struct PoolState {
  // The directory in which files are stored
  std::string path;

  // The (lowest, highest) sequence number of all existing files
  uint64_t file_list[2];

  // The sequence number of the current file
  uint64_t file_seq;

  // The file descriptor for the current file
  int file_fd;

  // The size of the current file in bytes
  size_t file_size;

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
