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

typedef struct {
  pthread_t       thread;
  int             queue[2];
  int             running;
  ev_loop_t       loop;
} worker_t;

worker_t* worker_init();
void* worker_run(void* userdata);
void worker_stop(worker_t* self);
void worker_cleanup(worker_t* self);
void worker_accept(worker_t* self);

#endif
