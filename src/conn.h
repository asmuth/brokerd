// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#ifndef CONN_H
#define CONN_H

#include <sys/socket.h>
#include "http.h"
#include "worker.h"

#define CONN_STATE_HEAD 1
#define CONN_STATE_BODY 2
#define CONN_STATE_STREAM 3
#define CONN_STATE_WAIT 4
#define CONN_STATE_FLUSH 5
#define CONN_STATE_CLOSED 6

typedef struct conn_s {
  int              state;
  int              sock;
  struct sockaddr* addr;
  socklen_t        addr_len;
  char*            buf;
  int              buf_len;
  int              buf_pos;
  int              buf_limit;
  http_req_t*      http_req;
  struct conn_s*   next;
  worker_t*        worker;
} conn_t;

conn_t* conn_init();
void conn_close();
int conn_read(conn_t* self);
int conn_read_head(conn_t* self);
int conn_write(conn_t* self);
int conn_write_flush(conn_t* self);
void conn_set_nonblock(conn_t* self);
void conn_reset(conn_t* self);
void conn_handle(conn_t* self);
void conn_handle_deliver(conn_t* self);
void conn_handle_ping(conn_t* self);
void conn_handle_404(conn_t* self);

#endif
