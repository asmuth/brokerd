/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#pragma once
#include <stdlib.h>
#include <string>
#include <list>
#include <brokerd/util/time.h>
#include <brokerd/util/return_code.h>

namespace brokerd {

typedef uint64_t FeedOffset;

struct Message {
  uint64_t offset;
  uint64_t next_offset;
  std::string data;
};

std::string toJSON(const std::list<Message>& messages);

ReturnCode messageWrite(
    const char* msg,
    size_t msg_len,
    int fd,
    size_t* msg_envelope_len);

} // namespace brokerd

