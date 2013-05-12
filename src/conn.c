// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>

#include "conn.h"

conn_t* conn_init() {
  conn_t* conn = (conn_t *) calloc(1, sizeof(conn_t));
  conn->addr_len = sizeof(conn->addr);
  return conn;
}

void conn_close(conn_t* conn) {
  close(conn->sock);
  free(conn);
}
