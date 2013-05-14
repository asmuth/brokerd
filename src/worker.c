// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <string.h>
#include <fcntl.h>

#include "worker.h"
#include "conn.h"

worker_t* worker_init() {
  int err;

  worker_t* worker = calloc(1, sizeof(worker));
  worker->connections = NULL;

  if (pipe(worker->queue) == -1) {
    printf("create pipe failed!\n");
    return NULL;
  }

  err = pthread_create(&worker->thread, NULL, worker_run, worker);

  if (err) {
    printf("error starting worker: %i\n", err);
    return NULL;
  }

  return worker;
}

void worker_stop(worker_t* worker) {
  printf("worker_stop\n");
  //pthread_join(worker->thread);
  free(worker);
}

void *worker_run(void* userdata) {
  int res;
  fd_set op_read, op_write;

  worker_t* self = userdata;
  conn_t* conn;

  while (1) {
    printf("worker selecting...\n");
    FD_ZERO(&op_read);

    for (conn = self->connections; conn != NULL; ) {
      printf("connection!!!!\n");

      // conn interest claim
      FD_SET(conn->sock, &op_read);
      // EOF conn interest claim

      conn = conn->next;
    }

    FD_SET(self->queue[0], &op_read);

    res = select(FD_SETSIZE, &op_read, NULL, NULL, NULL);

    if (res == 0) {
      printf("timeout while selecting\n");
      continue;
    }

    if (res == -1) {
      printf("error while selecting\n");
      continue;
    }

    printf("select(): %i\n", res);

    if (FD_ISSET(self->queue[0], &op_read)) {
      printf("new connection!\n");

      if (read(self->queue[0], &conn, sizeof(conn_t *)) != sizeof(conn_t *)) {
        printf("error reading from conn_queue\n");
        continue;
      }

      if (self->connections == NULL) {
        self->connections = conn;
      } else {
        conn->next = self->connections;
        self->connections = conn;
      }
    }


    for (conn = self->connections; conn != NULL; ) {
      // conn callback exec

      if (FD_ISSET(conn->sock, &op_read))
        conn_read(conn);

      // EOF conn callback exec

      conn = conn->next;
    }


    //printf("read next connection...\n");
    //proc_conn(conn);
  }

  return self;
}
