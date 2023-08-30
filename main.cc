#include <cstdlib>
#include <chrono>
#include <thread>
#include <fmt/core.h>

#include "storage/posixfs/pool.h"

int main(int argc, char** argv) {
  auto pool = pool_init(".");

  for (size_t i = 0; true; ++i) {
    auto msg = fmt::format("message #{}\n", i);
    if (!pool_write(&pool, (const uint8_t*) msg.data(), msg.size())) {
      return EXIT_FAILURE;
    }

    std::this_thread::sleep_for(std::chrono::microseconds(100));
  }

  return EXIT_SUCCESS;
}
