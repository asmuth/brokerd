// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#ifndef CONN_H
#define CONN_H

#include <sys/socket.h>

typedef struct {
  int              sock;
  struct sockaddr* addr;
  socklen_t        addr_len;
  char*            buf;
  int              buf_len;
} conn_t;

conn_t* conn_init();
void conn_close();

#endif
