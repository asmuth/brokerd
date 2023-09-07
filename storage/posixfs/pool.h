#pragma once
#include <string>

struct PoolState {
  std::string path;
  uint64_t segment_number;
  bool segment_active;
  size_t segment_size;
  size_t segment_size_limit;
  int segment_fd;
};

PoolState pool_init(
  const std::string& path
);

bool pool_write(
  PoolState* pool,
  const uint8_t* data,
  size_t data_len
);
