/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include "brokerd/RemoteFeed.h"
#include "brokerd/RemoteFeedFactory.h"

namespace stx {
namespace feeds {

RemoteFeedFactory::RemoteFeedFactory(
    RPCChannel* rpc_channel) :
    rpc_channel_(rpc_channel) {}

std::unique_ptr<RemoteFeed> RemoteFeedFactory::getFeed(
    const std::string& name) {
  return std::unique_ptr<RemoteFeed>(
      new stx::feeds::RemoteFeed(name, rpc_channel_));
}

}
}
