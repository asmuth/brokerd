// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdlib.h>
#include <stdio.h>
#include <sys/socket.h>
#include <unistd.h>
#include <fcntl.h>

#include "conn.h"
#include "http.h"

conn_t* conn_init(int buf_len) {
  conn_t* conn = (conn_t *) calloc(1, sizeof(conn_t));
  conn->addr_len = sizeof(conn->addr);
  conn->buf      = calloc(1, buf_len);
  conn->buf_len  = buf_len;
  conn->http_req = http_req_init();
  conn->next     = NULL;
  return conn;
}

void conn_close(conn_t* conn) {
  close(conn->sock);
  free(conn->buf);
  free(conn->http_req);
  free(conn);
}

void conn_set_nonblock(conn_t* conn) {
  int flags = fcntl(conn->sock, F_GETFL, 0);
  flags = flags & O_NONBLOCK;

  if (fcntl(conn->sock, F_SETFL, flags) != 0)
    printf("fnctl failed!\n");
}

void conn_read(conn_t* self) {
  int chunk, body_pos;

  chunk = read(self->sock, self->buf + self->buf_pos, 10);

  if (chunk == 0) {
    printf("read EOF\n");
    //conn_close(conn);
    return;
  }

  if (chunk < 0) {
    printf("error while reading...\n");
    //conn_close(conn);
    return;
  }

  self->buf_pos += chunk;

  body_pos = http_read(self->http_req, self->buf, self->buf_pos);

  if (body_pos == -1) {
    printf("http_read() returned error\n");
    //conn_close(conn);
  }

  if (body_pos > 0) {
    printf("http request complete!!!!!!!!!!!!!!!!!!!!\n");
  }

  //printf("http: %i %s\n", conn->http_req->method, conn->http_req->uri);

  //printf("write...\n");

  //char* resp = "HTTP/1.0 200 OK\r\nServer: fyrehose-v0.0.1\r\n\r\nfnord :)\r\n";
  //write(conn->sock, resp, strlen(resp));

  //printf("close...\n");
  //conn_close(conn);
}
