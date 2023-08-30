#include "pool.h"
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

PoolState pool_init(
  const std::string& path
) {
  PoolState p;
  p.path = path;
  p.segment_number = 1;
  p.segment_fd = -1;
  p.segment_size = 0;
  p.segment_size_limit = 2 << 10;
  return p;
}

bool pool_segment_open(
  PoolState* pool
) {
  // FIXME: allocation
  std::string segment_path;
  segment_path += pool->path;
  segment_path += "/";
  segment_path += std::to_string(pool->segment_number);
  segment_path += ".log";

  pool->segment_fd = ::open(
    segment_path.c_str(),
    O_WRONLY | O_CREAT | O_EXCL,
    0644
  );

  if (pool->segment_fd >= 0) {
    return true;
  } else {
    perror("unable to open file");
    return false;
  }
}

bool pool_segment_close(
  PoolState* pool
) {
  if (pool->segment_fd < 0) {
    return false;
  }

  if (::close(pool->segment_fd) == 0) {
    pool->segment_fd = -1;
    return true;
  } else {
    perror("close error");
    return false;
  }
}

bool pool_segment_conclude(
  PoolState* pool
) {
  if (!pool_segment_close(pool)) {
    return false;
  }

  pool->segment_number += 1;
  pool->segment_size = 0;
  return true;
}

bool pool_write(
  PoolState* pool,
  const uint8_t* data,
  size_t data_len
) {
  // Conclude the current segment file if writing would exceed the size limit
  if (
    auto segment_free_space = pool->segment_size_limit - pool->segment_size;
    data_len > segment_free_space
  ) {
    if (!pool_segment_conclude(pool)) {
      return false;
    }
  }

  // Open the current segment file if required
  if (pool->segment_fd < 0) {
    if (!pool_segment_open(pool)) {
      return false;
    }
  }

  // Write the data to the current segment file
  for (size_t write_pos = 0; write_pos < data_len; ) {
    auto write_result = ::write(
      pool->segment_fd,
      data + write_pos,
      data_len - write_pos
    );

    if (write_result >= 0) {
      write_pos += write_result;
      pool->segment_size += write_result;
    } else {
      perror("write error");
      return false;
    }
  }

  return true;
}
