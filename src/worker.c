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

extern int num_workers;
extern worker_t** workers;

worker_t* worker_init(int id) {
  int n;

  worker_t* worker = malloc(sizeof(worker_t));
  bzero(worker, sizeof(worker_t));

  worker->outbox = malloc(sizeof(rbuf_t *) * num_workers);

  for (n = 0; n < num_workers; n++) {
    if (n != id)
      worker->outbox[n] = rbuf_init(10); // FIXPAUL
  }

  worker->id = id;
  worker->running = 1;

  if (pipe(worker->conn_queue) == -1) {
    printf("create pipe failed!\n");
    return NULL;
  }

  if (fcntl(worker->conn_queue[0], F_SETFL, O_NONBLOCK) == -1)
    perror("fcntl(pipe, O_NONBLOCK)");

  if (pipe(worker->msg_queue) == -1) {
    printf("create pipe failed!\n");
    return NULL;
  }

  if (fcntl(worker->msg_queue[0], F_SETFL, O_NONBLOCK) == -1)
    perror("fcntl(pipe, O_NONBLOCK)");

  if (fcntl(worker->msg_queue[1], F_SETFL, O_NONBLOCK) == -1)
    perror("fcntl(pipe, O_NONBLOCK)");

  return worker;
}

void worker_start(worker_t* self) {
  int err;

  err = pthread_create(&self->thread, NULL, worker_run, self);

  if (err)
    printf("error starting worker: %i\n", err);
}

void worker_stop(worker_t* self) {
  int n, op = -1;
  void* ret;

  while (write(self->conn_queue[1], (char *) &op,
    sizeof(op)) != sizeof(op));

  pthread_join(self->thread, &ret);

  for (n = 0; n < num_workers; n++) {
    if (n != self->id)
      rbuf_free(self->outbox[n]);
  }

  free(self->outbox);
  free(self);
}

void worker_cleanup(worker_t* self) {
  int n, i, s;

  // FIXPAUL; this wont clean connections that are currently in the WAIT state...
  for (n = 0; n <= self->loop.max_fd; n++) {
    if (self->loop.events[n].userdata) {
      s = 1;

      for (i = 0; i < num_workers; i++) {
        if (workers[i] == self->loop.events[n].userdata)
          s = 0;
      }

      if (s == 1)
        conn_close((conn_t *) self->loop.events[n].userdata);
    }
  }

  ev_free(&self->loop);
}

void *worker_run(void* userdata) {
  int num_events, flush_outbox;
  worker_t* self = userdata;
  ev_event_t *event;
  conn_t *conn;

  ev_init(&self->loop);
  ev_watch(&self->loop, self->conn_queue[0], EV_READABLE, self);

  while(self->running) {
    num_events = ev_poll(&self->loop);
    flush_outbox = 0;

    if (num_events == -1)
      continue;

    while (--num_events >= 0) {
      event = self->loop.fired[num_events];

      if (!event->fired)
        continue;

      if (!event->userdata) {
        if (event->fired & EV_READABLE) {
          worker_flush_inbox(self);
        } else {
          flush_outbox = 1;
          continue;
        }

      } else if (event->userdata == self) {
        worker_accept(self);
        continue;
      }

      conn = event->userdata;

      if (event->fired & EV_READABLE)
        if (conn_read(conn) == -1)
          continue;

      if (event->fired & EV_WRITEABLE)
        if (conn_write(conn) == -1)
          continue;

    }

    if (flush_outbox)
      worker_flush_outbox(self);
  }

  worker_cleanup(self);
  return NULL;
}

inline void worker_flush_inbox(worker_t* self) {
  printf("flush outbox!\n");
}

inline void worker_flush_outbox(worker_t* self) {
  int n, pipe;
  void* data;
  ev_event_t* event;

  printf("flush outbox!\n");

  for (n = 0; n < num_workers; n++) {
    if (workers[n] == self)
      continue;

    if (self->outbox[n]->len < 1)
      continue;

    event = &self->loop.events[workers[n]->msg_queue[0]];

    if (!event->fired & EV_WRITEABLE)
      continue;

    printf("watcher has fired for worker %i\n", n);

    pipe = workers[n]->msg_queue[1];

    printf("pending messages for worker %i -> %i (fd %i)\n", self->id, n, pipe);
    // FIXPAUL: this could batch-write multiple entries at the same time!
    while (self->outbox[n]->len > 0) {
      printf("write single message to worker %i\n", n);
      data = rbuf_head(self->outbox[n]);
      printf(".... (%i)\n", pipe);

      if (write(pipe, &data, sizeof(data)) != sizeof(data)) {
        if (errno != EINTR && errno != EAGAIN && errno != EWOULDBLOCK)
          perror("error writing to worker->worker pipe");

        printf("interrupt...\n");
        break;
      }

      rbuf_pop(self->outbox[n]);
    }
  }
}

inline void worker_accept(worker_t* self) {
  conn_t *conn;
  int sock;

  ev_watch(&self->loop, self->conn_queue[0], EV_READABLE, self);

  if (read(self->conn_queue[0], &sock, sizeof(sock)) != sizeof(sock)) {
    if (errno != EINTR && errno != EAGAIN && errno != EWOULDBLOCK)
      printf("error reading from conn_queue\n");

    return;
  }

  if (sock == -1) {
    self->running = 0;
    return;
  }

  conn = conn_init(4096);
  conn->sock = sock;
  conn->worker = self;
  conn_set_nonblock(conn);
  conn_read(conn);
}
