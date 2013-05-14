// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#ifndef HTTP_H
#define HTTP_H

#define HTTP_STATE_METHOD 0
#define HTTP_STATE_URI 1
#define HTTP_STATE_VERSION 2
#define HTTP_STATE_HKEY 3
#define HTTP_STATE_HVAL 4

#define HTTP_METHOD_HEAD 0
#define HTTP_METHOD_GET 1
#define HTTP_METHOD_POST 2

typedef struct {
  int   method;
  char  uri[4096];
  int   state;
  int   last_pos;
  char* cur_token;
} http_req_t;

int http_read(http_req_t* req, char* buf, size_t len);
int http_read_method(http_req_t* req, char* start, char* end);
int http_read_uri(http_req_t* req, char* start, char* end);

http_req_t* http_req_init();
void http_req_free(http_req_t* self);

#endif
