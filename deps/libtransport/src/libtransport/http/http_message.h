/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *   Copyright (c) 2016 Paul Asmuth, FnordCorp B.V.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#pragma once
#include <vector>
#include <string>
#include <utility>

namespace libtransport {
namespace http {

class HTTPMessage {
public:
  typedef std::vector<std::pair<std::string, std::string>> HeaderList;

  enum kHTTPMethod {
    M_CONNECT,
    M_DELETE,
    M_GET,
    M_HEAD,
    M_OPTIONS,
    M_POST,
    M_PUT,
    M_TRACE
  };

  HTTPMessage() {}
  virtual ~HTTPMessage() {}

  const std::string& version() const;
  void setVersion(const std::string& version);

  const HeaderList& headers() const;
  const std::string& getHeader(const std::string& key) const;
  bool hasHeader(const std::string& key) const;
  void addHeader(const std::string& key, const std::string& value);
  void setHeader(const std::string& key, const std::string& value);
  void clearHeaders();

  const std::string& body() const;
  void addBody(const std::string& body);
  void addBody(const void* data, size_t size);
  void appendBody(const void* data, size_t size);
  void clearBody();

protected:
  std::string version_;
  static std::string kEmptyHeader;
  std::vector<std::pair<std::string, std::string>> headers_;
  std::string body_;
};

std::string getHTTPMethodName(HTTPMessage::kHTTPMethod method);

} // namespace http
} // namespace libtransport

