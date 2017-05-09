/**
 * This file is part of the "brokerd" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <brokerd/channel.h>
#include <brokerd/util/stringutil.h>

namespace brokerd {

Option<ChannelID> ChannelID::fromString(const std::string& s) {
  if (s.empty() || !StringUtil::isShellSafe(s)) {
    return None<ChannelID>();
  }

  return Some(ChannelID(s));
}

ChannelID::ChannelID(const std::string& id) : id_(id) {}

const std::string& ChannelID::str() const {
  return id_;
}

} // namespace brokerd

