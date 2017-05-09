/**
 * This file is part of the "libfnord" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <iostream>
#include <libtransport/json/json.h>
#include <brokerd/util/time.h>
#include <brokerd/util/logging.h>
#include <brokerd/util/buffer.h>
#include <brokerd/http_server.h>
#include <brokerd/channel_map.h>

namespace brokerd {

HTTPServer::HTTPServer(
    ChannelMap* channel_map) :
    channel_map_(channel_map) {
  http_server_.setRequestHandler(
      std::bind(
          &HTTPServer::handleRequest,
          this,
          std::placeholders::_1,
          std::placeholders::_2));
}

ReturnCode HTTPServer::listenAndRun(const std::string& addr, int port) {
  logInfo("Starting HTTP server on $0:$1", addr, port);

  if (!http_server_.listen(addr, port)) {
    return ReturnCode::error("ERUNTIME", "listen() failed");
  }

  http_server_.run();
  return ReturnCode::success();
}

void HTTPServer::handleRequest(
    http::HTTPRequest* req,
    http::HTTPResponse* res) try {
  URI uri(req->uri());
  auto path = uri.path();
  auto path_parts = StringUtil::split(path.substr(1), "/");

  res->addHeader("Access-Control-Allow-Origin", "*");

  switch (req->method()) {

    case http::HTTPMessage::M_GET:
      if (path == "/ping") {
        handleRequest_PING(req, res);
        return;
      }

      break;

    case http::HTTPMessage::M_POST:
      if (path_parts.size() == 2 && path_parts[0] == "channel") {
        handleRequest_INSERT(req, res, path_parts[1]);
      }
      break;
  }

  //if (StringUtil::endsWith(uri.path(), "/insert")) {
  //  return insertRecord(req, res, &uri);
  //}

  //if (StringUtil::endsWith(uri.path(), "/fetch")) {
  //  return fetchRecords(req, res, &uri);
  //}

  //if (StringUtil::endsWith(uri.path(), "/host_id")) {
  //  return getHostID(req, res, &uri);
  //}

  res->setStatus(http::kStatusNotFound);
  res->addBody("not found");
} catch (const Exception& e) {
  res->setStatus(http::kStatusInternalServerError);
  res->addBody(StringUtil::format("error: $0: $1", e.getTypeName(), e.getMessage()));
}

void HTTPServer::handleRequest_PING(
    http::HTTPRequest* req,
    http::HTTPResponse* res) {
  res->setStatus(http::kStatusOK);
  res->addHeader("Content-Type", "text/plain; charset=utf-8");
  res->addBody("pong");
}

void HTTPServer::handleRequest_INSERT(
    http::HTTPRequest* req,
    http::HTTPResponse* res,
    const std::string& channel_id) {
  if (req->body().empty()) {
    res->setStatus(http::kStatusBadRequest);
    res->addBody("error: empty message (body_size == 0)");
    return;
  }

  uint64_t offset;
  auto rc = channel_map_->appendMessage(channel_id, req->body(), &offset);
  if (!rc.isSuccess()) {
    res->setStatus(http::kStatusInternalServerError);
    res->addBody(StringUtil::format("error: $0", rc.getMessage()));
    return;
  }

  res->addHeader("X-Broker-HostID", channel_map_->getHostID());
  res->addHeader("X-Broker-Created-Offset", StringUtil::toString(offset));
  res->setStatus(http::kStatusCreated);
}

//void HTTPServer::getHostID(
//    http::HTTPRequest* req,
//    http::HTTPResponse* res,
//    URI* uri) {
//  res->addHeader("X-Broker-HostID", service_->hostID());
//  res->addHeader("Content-Type", "text/plain");
//  res->setStatus(http::kStatusOK);
//  res->addBody(service_->hostID());
//}

} // namespace brokerd

