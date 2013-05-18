// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "http.h"

#define HTTP_CUR_TOKEN req->cur_token, pos - req->cur_token

http_req_t* http_req_init() {
  http_req_t* req = calloc(1, sizeof(http_req_t));
  req->last_pos = 0;
  req->state = HTTP_STATE_METHOD;

  return req;
}

void http_req_free(http_req_t* self) {
  free(self);
}

// parses a http request
//
// can be called multiple times on the same buffer (with increasing lenghth)
// for incremental parsing... (it remembers the last_pos in req->last_pos)
//
// returns 0 if the request was parsed without errors so far, but is not yet
// finished and returns -1 if an error occured. if the request was successfully
// and completely parsed it will return the (positive non-zero) position of the
// first byte of the http request body
int http_read(http_req_t* req, char* buf, size_t len) {
  int n, nxt = len - req->last_pos - 1;
  char* pos  = buf + req->last_pos;

  for (n = 0; n <= nxt; n++) {
    switch (*pos) {

      case '\r':
        *pos = 0;
        break;

      case ' ':
        switch (req->state) {

          case HTTP_STATE_METHOD:
            if (http_read_method(req, buf, pos - buf) == -1)
              return -1;

            req->cur_token = pos + 1;
            break;

          case HTTP_STATE_URI:
            if (http_read_uri(req, HTTP_CUR_TOKEN) == -1)
              return -1;

            req->cur_token = pos + 1;
            break;

        }; break;

      case '\n':
        switch (req->state) {

          case HTTP_STATE_METHOD:
            return -1;

          case HTTP_STATE_URI:
            return -1;

          case HTTP_STATE_VERSION:
            if (http_read_version(req, HTTP_CUR_TOKEN) == -1)
              return -1;

            req->cur_token = pos + 1;
            break;

          case HTTP_STATE_HVAL:
            http_read_header(req,
              req->cur_hkey, req->cur_hkey_len,
              req->cur_token, pos - req->cur_token);

            req->state = HTTP_STATE_HKEY;
            req->cur_token = pos + 1;
            break;

          case HTTP_STATE_HKEY:
            return pos - buf;
            break;

      }; break;

      case ':':
        switch (req->state) {

          case HTTP_STATE_HKEY:
            // *pos = 0; printf("  >> hkey:   %s\n", req->cur_token);
            req->cur_hkey_len = pos - req->cur_token;
            req->cur_hkey = req->cur_token;
            req->cur_token = pos + 1;
            req->state = HTTP_STATE_HVAL;
            break;

      }; break;

    }

    pos++;
  }

  req->last_pos += nxt;

  return 0;
}

int http_read_method(http_req_t* req, char* method, int len) {
  if (strncmp(method, "HEAD", len) == 0)
    req->method = HTTP_METHOD_HEAD;

  else if (strncmp(method, "GET", len) == 0)
    req->method = HTTP_METHOD_GET;

  else if (strncmp(method, "POST", len) == 0)
    req->method = HTTP_METHOD_POST;

  else
    return -1;

  req->state = HTTP_STATE_URI;
  return 0;
}

int http_read_uri(http_req_t* req, char* uri, int len) {
  if (len >= sizeof(req->uri))
    return -1;

  strncpy(req->uri, uri, len);
  req->uri[len] = 0;

  req->state = HTTP_STATE_VERSION;
  return 0;
}


int http_read_version(http_req_t* req, char* version, int len) {
  if (len < 8)
    return -1;

  if (strncmp(version + 5, "1.1", len - 5) == 0)
    req->keepalive = 1;

  else
    req->keepalive = 0;

  req->state = HTTP_STATE_HKEY;
  return 0;
}

void http_read_header(http_req_t* req, char* hkey, int hkey_len, char* hval, int hval_len) {
  printf("header: %i, %i: \n", hkey_len, hval_len);
}

