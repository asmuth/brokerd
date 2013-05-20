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
#include <errno.h>

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

void conn_set_nonblock(conn_t* self) {
  int flags = fcntl(self->sock, F_GETFL, 0);
  flags = flags | O_NONBLOCK;

  if (fcntl(self->sock, F_SETFL, flags) != 0)
    printf("fnctl failed!\n");
}

int conn_read(conn_t* self) {
  switch (self->state) {

    case CONN_STATE_HEAD:
      return conn_read_head(self);

    default:
      printf("error: invalid conn-state (read): %i\n", self->state);

  }

  conn_close(self);
  return -1;
}

inline int conn_read_head(conn_t* self) {
  int chunk, body_pos;

  if (self->buf_len - self->buf_pos <= 0) {
    printf("error: http request buffer exhausted\n");
    conn_close(self);
    return -1;
  }

  chunk = read(self->sock, self->buf + self->buf_pos,
    self->buf_len - self->buf_pos);

  if (chunk == 0) {
    conn_close(self);
    return -1;
  }

  if (chunk < 0) {
    if (errno != EINTR && errno != EAGAIN && errno != EWOULDBLOCK) {
      perror("error while reading...");
      conn_close(self);
      return -1;
    }

    ev_watch(&self->worker->loop, self->sock, EV_READABLE, self);
    return 0;
  }

  self->buf_pos += chunk;
  body_pos = http_read(self->http_req, self->buf, self->buf_pos);

  if (body_pos == -1) {
    printf("http_read() returned error\n");
    conn_close(self);
    return -1;
  }

  if (body_pos > 0)
    conn_handle(self);
  else
    ev_watch(&self->worker->loop, self->sock, EV_READABLE, self);

  return 0;
}

int conn_write(conn_t* self) {
  switch (self->state) {

    case CONN_STATE_FLUSH:
      return conn_write_flush(self);

    default:
      printf("error: invalid conn-state (read): %i\n", self->state);

  }

  conn_close(self);
  return -1;
}

inline int conn_write_flush(conn_t* self) {
  int chunk;

  chunk = write(self->sock, self->buf + self->buf_pos,
    self->buf_limit - self->buf_pos);

  if (chunk == -1) {
    if (errno != EINTR && errno != EAGAIN && errno != EWOULDBLOCK) {
      perror("error while writing...");
      conn_close(self);
      return -1;
    }

    ev_watch(&self->worker->loop, self->sock, EV_WRITEABLE, self);
    return 0;
  }

  if (chunk > 0)
    self->buf_pos += chunk;

  if (self->buf_pos + 1 >= self->buf_limit) {
    if (self->http_req->keepalive) {
      conn_reset(self);
      conn_read(self);
      return 0;
    } else {
      conn_close(self);
      return -1;
    }
  }

  ev_watch(&self->worker->loop, self->sock, EV_WRITEABLE, self);
  return 0;
}

inline void conn_handle(conn_t* self) {
  int    argc = self->http_req->uri_argc;
  char** argv = self->http_req->uri_argv;

  int n;
  char buf[1024];

  printf("req... parts: %i\n", self->http_req->uri_argc);

  for (n = 0; n < argc; n++) {
    strncpy(buf, argv[n], argv[n+1]-argv[n]);
    buf[argv[n+1]-argv[n]] = 0;
    printf(">> arg(%i): '%s'\n", argv[n+1] - argv[n], buf);
  }

/*
  if (self->http_req->method == HTTP_METHOD_POST) {
    if (n 
  if (n == 1)



  if (p1 - url == 1)
    goto not_found;

  if (self->http_req->method == HTTP_METHOD_POST) {
    if (end - p1 > 1)
      goto not_found;

    printf("post to channel...\n");
    return;
  }

  if (end - p1 <= 1)
    goto get_actions;

  for (p2 = p1 + 1; p2 < end && *p2 != '/'; p2++);

  if (p1[1] == 's') {
    if ((end - p2) > 1)
      goto not_found;

    if (strncmp(p1 + 1, "stream", (p2 - p1) - 1) != 0)
      goto not_found;

    printf("handle stream...\n");
    return;
  }

  //*p1 = 0; printf("p1: %s\n", url + 1);
  //*p2 = 0; printf("p2: %s\n", p1 + 1);

get_actions:

  if (strncmp(url + 1, "ping", p1 - url - 1) == 0)
    return conn_handle_ping(self);
*/

not_found:
  conn_handle_404(self);

}

inline void conn_handle_ping(conn_t* self) {
  char* resp = "HTTP/1.1 200 OK\r\nServer: fyrehose-v0.0.1\r\nConnection: Keep-Alive\r\nContent-Length: 6\r\n\r\npong\r\n";

  self->state = CONN_STATE_FLUSH;
  self->buf_limit = strlen(resp);
  self->buf_pos = 0;

  strcpy(self->buf, resp);
  conn_write(self);
}

inline void conn_handle_404(conn_t* self) {
  char* resp = "HTTP/1.1 404 Not Found\r\nServer: fyrehose-v0.0.1\r\nConnection: Keep-Alive\r\nContent-Length: 11\r\n\r\nnot found\r\n";

  self->state = CONN_STATE_FLUSH;
  self->buf_limit = strlen(resp);
  self->buf_pos = 0;

  strcpy(self->buf, resp);
  conn_write(self);
}
