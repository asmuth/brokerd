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
#include <string.h>

#include "conn.h"
#include "http.h"
#include "worker.h"

conn_t* conn_init(int buf_len) {
  conn_t* conn = (conn_t *) malloc(sizeof(conn_t));
  bzero(conn, sizeof(conn_t));

  conn->addr_len = sizeof(conn->addr);
  conn->buf      = malloc(buf_len);
  conn->buf_len  = buf_len;
  conn->http_req = http_req_init();
  conn->next     = NULL;
  conn->state    = CONN_STATE_HEAD;
  return conn;
}

void conn_close(conn_t* self) {
  conn_t** cur = (conn_t **) &((worker_t *) self->worker)->connections;

  ev_unwatch(((worker_t *) self->worker)->ev_state, self, EV_WATCH_READ | EV_WATCH_WRITE);
  self->state = CONN_STATE_CLOSED;

  for (; (*cur)->sock != self->sock; cur = &(*cur)->next)
    if (!*cur) goto free;

  *cur = (*cur)->next;

  free:
  close(self->sock);
  http_req_free(self->http_req);
  free(self->buf);
  free(self);
}

void conn_set_nonblock(conn_t* conn) {
  int flags = fcntl(conn->sock, F_GETFL, 0);
  flags = flags & O_NONBLOCK;

  if (fcntl(conn->sock, F_SETFL, flags) != 0)
    printf("fnctl failed!\n");
}

int conn_read(conn_t* self) {
  int chunk, body_pos;

  if (self->buf_len - self->buf_pos <= 0) {
    printf("error: http request buffer exhausted\n");
    return -1;
  }

  chunk = read(self->sock, self->buf + self->buf_pos,
    self->buf_len - self->buf_pos);

  if (chunk == 0) {
    //printf("read EOF\n");
    return -1;
  }

  if (chunk < 0) {
    perror("error while reading...");
    return -1;
  }

  self->buf_pos += chunk;
  body_pos = http_read(self->http_req, self->buf, self->buf_pos);

  if (body_pos == -1) {
    printf("http_read() returned error\n");
    return -1;
  }

  if (body_pos > 0) {
    // FIXPAUL handle request here !
    self->state = CONN_STATE_STREAM;

    // STUB!!!
    char* resp = "HTTP/1.1 200 OK\r\nServer: fyrehose-v0.0.1\r\nConnection: Keep-Alive\r\nContent-Length: 10\r\n\r\nfnord :)\r\n";
    self->buf_limit = strlen(resp);
    self->buf_pos = 0;
    strcpy(self->buf, resp);
    // EOF STUB
  }

  return 0;
}

int conn_write(conn_t* self) {
  int chunk;

  chunk = write(self->sock, self->buf + self->buf_pos,
    self->buf_limit - self->buf_pos);

  if (chunk == -1) {
    perror("write returned an error");
    return -1;
  }

  if (chunk > 0)
    self->buf_pos += chunk;

  if (self->buf_pos + 1 >= self->buf_limit) {
    if (self->http_req->keepalive) {
      self->buf_pos = 0;
      self->state = CONN_STATE_HEAD;
    } else {
      return -1;
    }
  }

  return 0;
}
