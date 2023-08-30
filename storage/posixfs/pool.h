#pragma once
#include <string>

struct PoolState {
  std::string path;
  uint64_t segment_number;
  int segment_fd;
  size_t segment_size;
  size_t segment_size_limit;
};

PoolState pool_init(
  const std::string& path
);

bool pool_write(
  PoolState* pool,
  const uint8_t* data,
  size_t data_len
);
