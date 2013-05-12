// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>
#include <pthread.h>

#include "server.h"
#include "worker.h"
#include "conn.h"

void conn_close(conn_t* conn) {
  close(conn->sock);
  free(conn);
}


void *work(void* fnord) {
  conn_t* conn;

  while (1) {
    printf("worker waiting...\n");

    if (read(conn_queue[0], &conn, sizeof(conn_t *)) != sizeof(conn_t *)) {
      printf("error reading from conn_queue\n");
      continue;
    }

    printf("read next connection...\n");

    printf("write...\n");
    write(conn->sock, "fnord\n", 6);

    printf("close...\n");
    conn_close(conn);
  }

  return fnord;
}

int worker_init(pthread_t* thread) {
  int err;

  err = pthread_create(thread, NULL, work, NULL);

  if (err) {
    printf("error starting worker: %i\n", err);
    return -1;
  }

  return 0;
}
