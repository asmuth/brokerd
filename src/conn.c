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

conn_t* conn_init(int buf_len) {
  conn_t* conn = (conn_t *) calloc(1, sizeof(conn_t));
  conn->buf      = calloc(1, buf_len);
  conn->buf_len  = buf_len;
  conn->addr_len = sizeof(conn->addr);
  return conn;
}

void conn_close(conn_t* conn) {
  close(conn->sock);
  free(conn->buf);
  free(conn);
}

void conn_set_nonblock(conn_t* conn) {
  int flags = fcntl(conn->sock, F_GETFL, 0);
  flags = flags & O_NONBLOCK;

  if (fcntl(conn->sock, F_SETFL, flags) != 0)
    printf("fnctl failed!\n");
}
