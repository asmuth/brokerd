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

#include "ev.h"
#include "worker.h"
#include "conn.h"

worker_t* worker_init() {
  int err;

  worker_t* worker = malloc(sizeof(worker_t));
  bzero(worker, sizeof(worker_t));

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
  int num_events, sock;
  worker_t* self = userdata;
  conn_t *conn;
  ev_event_t *event;

  ev_init(&self->loop);
  ev_watch(&self->loop, self->queue[0], EV_READABLE, NULL);

  while (1) {
   num_events = ev_poll(&self->loop);

   if (num_events == -1)
      continue;

    while (--num_events >= 0) {
      event = self->loop.fired[num_events];

      if (!event->fired)
        continue;

      if (event->userdata != NULL) {
        conn = event->userdata;

        if (event->fired & EV_READABLE)
          if (conn_read(conn) == -1) {
            conn_close(conn);
            continue;
          }

        if (event->fired & EV_WRITEABLE)
          if (conn_write(conn) == -1) {
            conn_close(conn);
            continue;
          }
      }

      // pops the next connection from the queue
      else {
        ev_watch(&self->loop, self->queue[0], EV_READABLE, NULL);

        if (read(self->queue[0], &sock, sizeof(sock)) != sizeof(sock)) {
          if (errno != EINTR && errno != EAGAIN && errno != EWOULDBLOCK)
            printf("error reading from conn_queue\n");

          continue;
        }

        conn = conn_init(4096);
        conn->sock = sock;
        conn->worker = self;
        conn_set_nonblock(conn);
        ev_watch(&self->loop, conn->sock, EV_READABLE, conn);
      }
    }
  }
}
