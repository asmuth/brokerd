#include "pool.h"
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <fmt/core.h>

PoolState pool_init(
  const std::string& path
) {
  PoolState p;
  p.path = path;
  p.segment_number = 1;
  p.segment_active = false;
  p.segment_size = 0;
  p.segment_size_limit = 2 << 10;
  p.segment_fd = -1;
  return p;
}

bool pool_segment_allocate (
  PoolState* pool
) {
  if (pool->segment_active) {
    fmt::print(stderr, "ERROR: segment not concluded\n");
    return false;
  }

  pool->segment_number++;
  pool->segment_active = true;
  pool->segment_size = 0;

  return true;
}

bool pool_segment_conclude(
  PoolState* pool
) {
  if (!pool->segment_active) {
    fmt::print(stderr, "ERROR: segment not allocated\n");
    return false;
  }

  pool->segment_active = false;
  return true;
}

size_t pool_segment_free_space(const PoolState& pool) {
  if (!pool.segment_active) {
    return 0;
  }

  if (pool.segment_size > pool.segment_size_limit) {
    return 0;
  }

  return pool.segment_size_limit - pool.segment_size;
}

bool pool_segment_open(
  PoolState* pool
) {
  if (!pool->segment_active) {
    fmt::print(stderr, "ERROR: segment not allocated\n");
    return false;
  }

  // @malloc
  auto segment_path = fmt::format(
    "{}/{:09}.log",
    pool->path,
    pool->segment_number
  );

  pool->segment_fd = ::open(
    segment_path.c_str(),
    O_WRONLY | O_CREAT | O_EXCL,
    0644
  );

  if (pool->segment_fd >= 0) {
    return true;
  } else {
    perror("unable to open segment file");
    return false;
  }
}

bool pool_segment_close(
  PoolState* pool
) {
  if (!pool->segment_active) {
    fmt::print(stderr, "ERROR: segment not allocated\n");
    return false;
  }

  if (pool->segment_fd < 0) {
    fmt::print(stderr, "ERROR: segment not open\n");
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

bool pool_write(
  PoolState* pool,
  const uint8_t* data,
  size_t data_len
) {
  // Conclude the current segment file if writing would exceed the size limit
  if (pool->segment_active && data_len > pool_segment_free_space(*pool)) {
    if (pool->segment_fd >= 0) {
      if (!pool_segment_close(pool)) {
        return false;
      }
    }

    if (!pool_segment_conclude(pool)) {
      return false;
    }
  }

  // Allocate a new segment if required
  if (!pool->segment_active) {
    if (!pool_segment_allocate(pool)) {
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
