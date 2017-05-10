/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <unistd.h>
#include <cstring>
#include <brokerd/message.h>
#include <brokerd/util/varint.h>
#include <libtransport/json/json.h>
#include <libtransport/json/json_writer.h>

namespace brokerd {

namespace json = libtransport::json;

std::string toJSON(const std::list<Message>& messages) {
  std::string json_str;
  json::JSONWriter json(&json_str);

  json.beginArray();
  for (const auto& m : messages) {
    json.beginObject();
    json.addString("offset");
    json.addInteger(m.offset);
    json.addString("next_offset");
    json.addInteger(m.next_offset);
    json.addString("data");
    json.addString(m.data);
    json.endObject();
  }
  json.endArray();

  return json_str;
}

ReturnCode messageWrite(
    const char* msg,
    size_t msg_len,
    int fd,
    size_t* msg_envelope_len) {
  std::string buf;
  writeVarUInt(&buf, msg_len);
  buf.append(msg, msg_len);

  int rc = ::write(fd, buf.data(), buf.size());
  if (rc < 0 || rc != buf.size()) {
    return ReturnCode::errorf("EIO", "write() failed: $0", strerror(errno));
  }

  *msg_envelope_len = buf.size();
  return ReturnCode::success();
}

} // namespace brokerd

