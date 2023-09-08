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
  p.file_list[1] = 0;
  p.file_list[0] = 0;
  p.file_size_limit = 2 << 10;
  p.file_count_limit = 8;
  return p;
}

std::string pool_file_path(const PoolState& pool, uint64_t n) {
  // @malloc
  return fmt::format("{}/{:09}.log", pool.path, n);
}

uint64_t pool_file_count(const PoolState& pool) {
  return pool.file_list[1] - pool.file_list[0];
}

size_t pool_file_free_space(const PoolState& pool) {
  if (!pool.file) {
    return 0;
  }

  if (pool.file->size > pool.file_size_limit) {
    return 0;
  }

  return pool.file_size_limit - pool.file->size;
}

bool pool_file_allocate(PoolState* pool) {
  if (pool->file) {
    fmt::print(stderr, "ERROR: file not concluded\n");
    return false;
  }

  pool->file = FileState{};
  pool->file->seq = ++pool->file_list[1];
  pool->file->size = 0;
  pool->file->fd = -1;

  return true;
}

bool pool_file_conclude(PoolState* pool) {
  if (!pool->file) {
    fmt::print(stderr, "ERROR: file not allocated\n");
    return false;
  }

  pool->file = std::nullopt;
  return true;
}

bool pool_file_open(PoolState* pool) {
  if (!pool->file) {
    fmt::print(stderr, "ERROR: file not allocated\n");
    return false;
  }

  auto file_path = pool_file_path(*pool, pool->file->seq);

  pool->file->fd = ::open(
    file_path.c_str(),
    O_WRONLY | O_CREAT | O_EXCL,
    0644
  );

  if (pool->file->fd >= 0) {
    return true;
  } else {
    perror("unable to open file");
    return false;
  }
}

bool pool_file_close(PoolState* pool) {
  if (!pool->file) {
    fmt::print(stderr, "ERROR: file not allocated\n");
    return false;
  }

  if (pool->file->fd < 0) {
    fmt::print(stderr, "ERROR: file not open\n");
    return false;
  }

  if (::close(pool->file->fd) == 0) {
    pool->file->fd = -1;
    return true;
  } else {
    perror("close error");
    return false;
  }
}

bool pool_rotate(
  PoolState* pool,
  uint64_t reserve = 0
) {
  while (pool_file_count(*pool) + reserve >= pool->file_count_limit) {
    auto file_path = pool_file_path(*pool, pool->file_list[0]);
    if (::unlink(file_path.c_str()) != 0) {
      switch (errno) {
        case ENOENT:
          break;
        default:
          perror("delete error");
          return false;
      }
    }

    pool->file_list[0]++;
  }

  return true;
}

bool pool_write(
  PoolState* pool,
  const uint8_t* data,
  size_t data_len
) {
  // Conclude the current file if writing would exceed the size limit
  if (pool->file && data_len > pool_file_free_space(*pool)) {
    if (pool->file->fd >= 0) {
      if (!pool_file_close(pool)) {
        return false;
      }
    }

    if (!pool_file_conclude(pool)) {
      return false;
    }
  }

  // Allocate a new file sequence number if required
  if (!pool->file) {
    if (!pool_rotate(pool, 1)) {
      return false;
    }

    if (!pool_file_allocate(pool)) {
      return false;
    }
  }

  // Open the current file if required
  if (pool->file->fd < 0) {
    if (!pool_file_open(pool)) {
      return false;
    }
  }

  // Write the data to the current file
  for (size_t write_pos = 0; write_pos < data_len; ) {
    auto write_result = ::write(
      pool->file->fd,
      data + write_pos,
      data_len - write_pos
    );

    if (write_result >= 0) {
      write_pos += write_result;
      pool->file->size += write_result;
    } else {
      perror("write error");
      return false;
    }
  }

  return true;
}
