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
#include "chan.h"
#include "msg.h"

conn_t* conn_init(int buf_len) {
  conn_t* conn = (conn_t *) malloc(sizeof(conn_t));
  bzero(conn, sizeof(conn_t));

  conn->addr_len  = sizeof(conn->addr);
  conn->buf       = malloc(buf_len);
  conn->buf_len   = buf_len;
  conn->http_req  = http_req_init();
  conn->next_sub  = NULL;
  conn->rbuf      = NULL;
  conn->state     = CONN_STATE_HEAD;
  conn->write_pos = 0;

  return conn;
}

void conn_close(conn_t* self) {
  ev_unwatch(&self->worker->loop, self->sock);
  self->state = CONN_STATE_CLOSED;

  if (self->channel)
    chan_unsubscribe(self->channel, self);

  if (self->rbuf) {
    while(self->rbuf->len > 0) {
      msg_decref(rbuf_head(self->rbuf));
      rbuf_pop(self->rbuf);
    }

    rbuf_free(self->rbuf);
  }

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
    case CONN_STATE_FLUSHWAIT:
      return conn_write_flush(self);

    case CONN_STATE_STREAM:
    case CONN_STATE_STREAMWAIT:
      return conn_write_stream(self);

    default:
      printf("error: invalid conn-state (write): %i\n", self->state);

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
    if (self->state == CONN_STATE_FLUSHWAIT) {
      //printf("now wait...\n");
      self->state = CONN_STATE_WAIT;
      return 0;
    } if (self->http_req->keepalive) {
      conn_reset(self);
      ev_watch(&self->worker->loop, self->sock, EV_READABLE, self);
      return 0;
    } else {
      conn_close(self);
      return -1;
    }
  }

  ev_watch(&self->worker->loop, self->sock, EV_WRITEABLE, self);
  return 0;
}

inline int conn_write_stream(conn_t* self) {
  int chunk;

  for (;;) {
    msg_t* msg = rbuf_head(self->rbuf);

    if (msg == NULL) {
      printf("fnoooooord!\n"); // FIXPAUL
      return -1;
    }

    chunk = write(self->sock, msg->data + self->write_pos,
      msg->len - self->write_pos);

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
      self->write_pos += chunk;

    if (!(self->write_pos + 1 >= msg->len))
      break;

    //printf("finish message!\n");
    self->write_pos = 0;
    rbuf_pop(self->rbuf);
    msg_decref(msg);

    if (self->rbuf->len > 0)
      continue;

    //printf("now wait again...\n");

    self->state = CONN_STATE_WAIT;
    return 0;
  }

  ev_watch(&self->worker->loop, self->sock, EV_WRITEABLE, self);
  return 0;
}


#define conn_http_argv_eq(N, S, L) ((argv[(N) + 1] - argv[(N)]) == (L) && \
  strncmp(argv[(N)], (S), (argv[(N) + 1] - argv[(N)])) == 0)

inline void conn_handle(conn_t* self) {
  int n, argc = self->http_req->uri_argc;
  char** argv = self->http_req->uri_argv;

  /*
  char buf[1024];
  printf("req... parts: %i\n", self->http_req->uri_argc);
  for (n = 0; n < argc; n++) {
    strncpy(buf, argv[n], argv[n+1]-argv[n]); buf[argv[n+1]-argv[n]] = 0;
    printf(">> arg(%i): '%s'\n", argv[n+1] - argv[n], buf);
  }
  */

  if (self->http_req->method == HTTP_METHOD_POST) {
    if (argc != 1)
      goto not_found;

    return conn_handle_deliver(self);
  }

  else if (argc == 0)
    goto not_found;

  else if (argc == 1)
    goto get_actions;

  else if (argc == 2 && conn_http_argv_eq(1, "/subscribe", 10))
    return conn_handle_subscribe(self);

get_actions:

  if (argc == 1 && conn_http_argv_eq(0, "/ping", 5))
    return conn_handle_ping(self);

not_found:

  conn_handle_404(self);

}

inline void conn_handle_subscribe(conn_t* self) {
  char* resp = "HTTP/1.1 200 FNORD\r\nServer: fyrehose-v0.0.1\r\nConnection: Keep-Alive\r\n\r\n";

  char* chan_key = self->http_req->uri_argv[1];
  int   chan_len = self->http_req->uri_argv[2] - chan_key;

  chan_t* chan = chan_lookup(chan_key, chan_len);
  chan_subscribe(chan, self);

  self->state = CONN_STATE_FLUSHWAIT;
  self->buf_limit = strlen(resp);
  self->buf_pos = 0;

  strcpy(self->buf, resp);
  conn_write(self);
}

inline void conn_handle_deliver(conn_t* self) {
  char* resp1 = "HTTP/1.1 201 Created\r\nServer: fyrehose-v0.0.1\r\nConnection: Keep-Alive\r\nContent-Length: 4\r\n\r\nok\r\n";
  char* resp2 = "fnordyfnord! :)\r\n";

  char* chan_key = self->http_req->uri_argv[1];
  int   chan_len = self->http_req->uri_argv[2] - chan_key;

  chan_t* chan = chan_lookup(chan_key, chan_len);
  msg_t*  msg  = msg_init(strlen(resp2));

  strncpy(msg->data, resp2, msg->len);

  int n;
  for (n = 0; n < 3; n++) {
    if (chan_deliver(chan, msg, self->worker) == -1) {
      printf("ERROR DELIVERING MESSAGE - FIXPAUL\n");
    }
  }

  msg_decref(msg);

  self->state = CONN_STATE_FLUSH;
  self->buf_limit = strlen(resp1);
  self->buf_pos = 0;

  strcpy(self->buf, resp1);
  conn_write(self);
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
