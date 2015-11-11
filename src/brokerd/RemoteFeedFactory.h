/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#ifndef _FNORD_FEEDS_FEEDFACTORY_H
#define _FNORD_FEEDS_FEEDFACTORY_H
#include <functional>
#include <stdlib.h>
#include <string>
#include <unordered_map>
#include <vector>
#include <mutex>
#include "stx/rpc/RPC.h"
#include "brokerd/RemoteFeed.h"

namespace stx {
namespace feeds {

class RemoteFeedFactory {
public:
  RemoteFeedFactory(RPCChannel* rpc_channel);
  std::unique_ptr<RemoteFeed> getFeed(const std::string& name);
protected:
  RPCChannel* rpc_channel_;
};

}
}
#endif
