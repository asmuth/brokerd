/**
 * This file is part of the "libfnord" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#pragma once
#include <brokerd/util/return_code.h>
#include <libtransport/http/v1/http_server.h>
#include <libtransport/http/http_request.h>
#include <libtransport/http/http_response.h>

namespace brokerd {
class ChannelMap;

namespace http = libtransport::http;

class HTTPServer {
public:

  HTTPServer(ChannelMap* channel_map);

  ReturnCode listenAndRun(const std::string& addr, int port);

protected:

  void handleRequest(
      http::HTTPRequest* req,
      http::HTTPResponse* res);

  void handleRequest_INSERT(
      http::HTTPRequest* req,
      http::HTTPResponse* res,
      const std::string& channel_id);

  void handleRequest_FETCH(
      http::HTTPRequest* req,
      http::HTTPResponse* res,
      const std::string& channel_id,
      const std::string& offset = "",
      bool next = false,
      const std::string& batch_size = "");

  void handleRequest_STATS(
      http::HTTPRequest* req,
      http::HTTPResponse* res);

  void handleRequest_SERVERID(
      http::HTTPRequest* req,
      http::HTTPResponse* res);

  void handleRequest_PING(
      http::HTTPRequest* req,
      http::HTTPResponse* res);

  libtransport::http::HTTPServer http_server_;
  ChannelMap* channel_map_;
};

} // namespace brokerd

