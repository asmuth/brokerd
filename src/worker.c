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
#include <errno.h>

#include "worker.h"
#include "conn.h"

worker_t* worker_init() {
  int err;

  worker_t* worker = malloc(sizeof(worker_t));
  bzero(worker, sizeof(worker_t));

  // FIXPAUL: make this non-blocking
  if (pipe(worker->queue) == -1) {
    printf("create pipe failed!\n");
    return NULL;
  }

  if (fcntl(worker->queue[0], F_SETFL, O_NONBLOCK) == -1) {
    perror("fcntl(pipe, O_NONBLOCK)");
  }

  err = pthread_create(&worker->thread, NULL, worker_run, worker);

  if (err) {
    printf("error starting worker: %i\n", err);
    return NULL;
  }

  return worker;
}

void worker_stop(worker_t* worker) {
  //printf("worker_stop\n");
  //pthread_join(worker->thread);
  free(worker);
}

void *worker_run(void* userdata) {
  int res;

  worker_t* self = userdata;
  conn_t *conn, *next;

  self->ev_state = ev_init();

  while (1) {
    ev_watch_fd(self->ev_state, self->queue[0], EV_WATCH_READ);

    for (conn = self->connections; conn != NULL; ) {
      switch (conn->state) {

        case CONN_STATE_HEAD:
          //FD_SET(conn->sock, &op_read);
          ev_watch(self->ev_state, conn, EV_WATCH_READ);
          break;

        case CONN_STATE_STREAM:
          //FD_SET(conn->sock, &op_write);
          ev_watch(self->ev_state, conn, EV_WATCH_WRITE);
          break;

      }

      conn = conn->next;
    }

    printf("select...\n");
    res = select(FD_SETSIZE, &self->ev_state->op_read, &self->ev_state->op_write, NULL, NULL); // FIXSELECT

    if (res == 0) {
      printf("timeout while selecting\n");
      continue;
    }

    if (res == -1) {
      perror("error while selecting");
      continue;
    }

    // pops the next connection from the queue
    if (FD_ISSET(self->queue[0], &self->ev_state->op_read)) { // FIXSELECT
      printf("new connection!\n");

      int fd;
      if (read(self->queue[0], &fd, sizeof(fd)) != sizeof(fd)) {
        if (errno != EINTR && errno != EAGAIN && errno != EWOULDBLOCK)
          printf("error reading from conn_queue\n");

        continue;
      }

      conn = conn_init(4096);
      conn->sock = fd;
      conn->worker = self;
      conn_set_nonblock(conn);

      if (self->connections == NULL) {
        self->connections = conn;
      } else {
        conn->next = self->connections;
        self->connections = conn;
      }
    }

    for (next = self->connections; next != NULL; ) {
      conn = next;
      next = conn->next;

      printf("callback!\n");

      if (FD_ISSET(conn->sock, &self->ev_state->op_read)) // FIXSELECT
        if (conn_read(conn) == -1) {
          conn_close(conn);
          continue;
        }

      if (FD_ISSET(conn->sock, &self->ev_state->op_write)) // FIXSELECT
        if (conn_write(conn) == -1) {
          conn_close(conn);
          continue;
        }

      ev_unwatch(self->ev_state, conn, EV_WATCH_READ | EV_WATCH_WRITE);
    }
  }

  return self;
}
