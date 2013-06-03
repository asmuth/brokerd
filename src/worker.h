// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#ifndef EWOULDBLOCK
#define EWOULDBLOCK EAGAIN
#endif

#ifndef WORKER_H
#define WORKER_H

#include <pthread.h>

#include "ev.h"
#include "rbuf.h"

typedef struct {
  int             id;
  pthread_t       thread;
  int             conn_queue[2];
  int             msg_queue[2];
  int             running;
  rbuf_t**        outbox;
  ev_loop_t       loop;
} worker_t;

worker_t* worker_init(int id);
void* worker_run(void* userdata);
void worker_start(worker_t* self);
void worker_stop(worker_t* self);
void worker_cleanup(worker_t* self);
void worker_accept(worker_t* self);
void worker_flush_outbox(worker_t* self);
void worker_flush_inbox(worker_t* self);

#endif
