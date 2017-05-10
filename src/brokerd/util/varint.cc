/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2016 Paul Asmuth <paul@asmuth.com>
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include "varint.h"

bool writeVarUInt(std::string* str, uint64_t value) {
  unsigned char buf[10];
  size_t bytes = 0;
  do {
    buf[bytes] = value & 0x7fU;
    if (value >>= 7) buf[bytes] |= 0x80U;
    ++bytes;
  } while (value);

  str->append((char*) buf, bytes);
  return true;
}

bool writeVarUInt(std::ostream* os, uint64_t value) {
  do {
    unsigned char b = value & 0x7fU;
    if (value >>= 7) b |= 0x80U;
    os->put(b);
  } while (value);

  return !os->fail();
}

bool readVarUInt(const char** cursor, const char* end, uint64_t* value) {
  *value = 0;

  for (int i = 0; ; ++i) {
    if (*cursor == end) {
      return false;
    }

    unsigned char b = *(*cursor)++;
    *value |= (b & 0x7fULL) << (7 * i);
    if (!(b & 0x80U)) {
      break;
    }
  }

  return true;
}

bool readVarUInt(std::istream* is, uint64_t* value) {
  *value = 0;

  for (int i = 0; ; ++i) {
    if (!is->good()) {
      return false;
    }

    unsigned char b = is->get();
    *value |= (b & 0x7fULL) << (7 * i);
    if (!(b & 0x80U)) {
      break;
    }
  }

  return true;
}
