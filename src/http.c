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

http_req_t* http_req_init() {
  http_req_t* req = calloc(1, sizeof(http_req_t));
  req->last_pos = 0;
  req->state = HTTP_STATE_METHOD;

  return req;
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
            if (http_read_method(req, buf, pos - 1) == -1)
              return -1;

            req->cur_token = pos + 1;
            break;

          case HTTP_STATE_URI:
            if (http_read_uri(req, req->cur_token, pos - 1) == -1)
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
            *pos = 0; printf("  >> http:   %s\n", req->cur_token);

            req->cur_token = pos + 1;
            req->state = HTTP_STATE_HKEY;

          case HTTP_STATE_HVAL:
            *pos = 0; printf("  >> hval:   %s\n", req->cur_token);

            req->cur_token = pos + 1;
            req->state = HTTP_STATE_HKEY;
            break;

          case HTTP_STATE_HKEY:
            printf("YEAH!\n");
            return pos - buf;
            break;

      }; break;

      case ':':
        switch (req->state) {

          case HTTP_STATE_HKEY:
            *pos = 0; printf("  >> hkey:   %s\n", req->cur_token);

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

int http_read_method(http_req_t* req, char* start, char* end) {
  char buf[64];
  int  len = end - start + 1;

  if (len >= 64)
    return -1;

  strncpy(buf, start, len);
  buf[len] = 0;

  printf("  >> method: %s\n", buf);

  req->state = HTTP_STATE_URI;

  return 0;
}

int http_read_uri(http_req_t* req, char* start, char* end) {
  char buf[64];
  int  len = end - start + 1;

  if (len >= 64)
    return -1;

  strncpy(buf, start, len);
  buf[len] = 0;

  printf("  >> uri:    %s\n", buf);

  req->state = HTTP_STATE_VERSION;

  return 0;
}
