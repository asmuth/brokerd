// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdlib.h>
#include <stdio.h>
#include <sys/socket.h>
#include <netinet/tcp.h>
#include <netinet/in.h>
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
  ev_unwatch(&self->worker->loop, self->sock);
  self->state = CONN_STATE_CLOSED;

  close(self->sock);
  http_req_free(self->http_req);
  free(self->buf);
  free(self);
}

void conn_reset(conn_t* self) {
  self->state = CONN_STATE_HEAD;
  self->buf_pos = 0;
  http_req_reset(self->http_req);
}

void conn_set_nonblock(conn_t* conn) {
  int opt = 1;
  int flags = fcntl(conn->sock, F_GETFL, 0);
  flags = flags | O_NONBLOCK;

  if (setsockopt(conn->sock, IPPROTO_TCP, TCP_NODELAY, &opt, sizeof(opt)) < 0)
    perror("setsockopt(TCP_NODELAY)");

  if (fcntl(conn->sock, F_SETFL, flags) != 0)
    printf("fnctl failed!\n");
}

int conn_read(conn_t* self) {
  int chunk, body_pos;

  switch (self->state) {

    case CONN_STATE_HEAD:
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
        // if (post_body_pending) {
        //   self->state = CONN_STATE_BODY;
        //   conn_read(self); // opportunistic read
        //   return 0;
        // else

        conn_handle(self);
      } else {
        ev_watch(&self->worker->loop, self->sock, EV_READABLE, self);
      }
      break;

  }

  return 0;
}

int conn_write(conn_t* self) {
  int chunk;

  switch (self->state) {

    case CONN_STATE_FLUSH:
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
          conn_reset(self);
          conn_read(self);
          return 0;
        } else {
          return -1;
        }
      }

      ev_watch(&self->worker->loop, self->sock, EV_WRITEABLE, self);
      break;

  }

  return 0;
}


void conn_handle(conn_t* self) {
  char*  url = self->http_req->uri;
  size_t url_len = sizeof(self->http_req->uri);

  printf("req: %s\n", url);

  if (strncmp(url, "/ping", url_len) == 0)
    conn_handle_ping(self);

  else
    conn_handle_404(self);
}

void conn_handle_ping(conn_t* self) {
  char* resp = "HTTP/1.1 200 OK\r\nServer: fyrehose-v0.0.1\r\nConnection: Keep-Alive\r\nContent-Length: 6\r\n\r\npong\r\n";

  self->state = CONN_STATE_FLUSH;
  self->buf_limit = strlen(resp);
  self->buf_pos = 0;

  strcpy(self->buf, resp);
  conn_write(self); // <--- opportunistic write :)
}

void conn_handle_404(conn_t* self) {
  char* resp = "HTTP/1.1 404 Not Found\r\nServer: fyrehose-v0.0.1\r\nConnection: Keep-Alive\r\nContent-Length: 11\r\n\r\nnot found\r\n";

  self->state = CONN_STATE_FLUSH;
  self->buf_limit = strlen(resp);
  self->buf_pos = 0;

  strcpy(self->buf, resp);
  conn_write(self);
}
