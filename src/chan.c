// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdlib.h>
#include <pthread.h>
#include <string.h>

#include "chan.h"

extern int num_workers;
chan_t* global_channel;

chan_t* chan_init(/*char* key, int key_len*/) {
  chan_t* self  = malloc(sizeof(chan_t *));
  self->sublist = malloc(sizeof(conn_t *) * num_workers);

  memset(self->sublist, 0, sizeof(conn_t *) * num_workers);
  pthread_mutex_init(&self->lock, NULL);

  return self;
}

chan_t* chan_lookup(char* key, int key_len) {
  return global_channel;
}

void chan_subscribe(chan_t* self, conn_t* conn) {
  int wid = conn->worker->id;

  pthread_mutex_lock(&self->lock);
  // subscribe worker...
  pthread_mutex_unlock(&self->lock);

  conn->channel      = self;
  conn->next_sub     = self->sublist[wid];
  self->sublist[wid] = conn;
}

void chan_unsubscribe(chan_t* self, conn_t* conn) {
  pthread_mutex_lock(&self->lock);
  // unsubscribe worker
  pthread_mutex_unlock(&self->lock);

  printf("close!\n");

  conn_t** cur = &self->sublist[conn->worker->id];
  conn->channel = NULL;

  while(*cur != NULL && (*cur)->sock != conn->sock)
    cur = &((*cur)->next_sub);

  if (*cur)
    *cur = (*cur)->next_sub;

}

void chan_deliver(chan_t* self, worker_t* worker) {
  conn_t* cur = self->sublist[worker->id];

  char* resp = "fnord! :)\r\n";

  for(; cur != NULL; cur = cur->next_sub) {
    printf("deliver local...\n");
    // STUB
    cur->state = CONN_STATE_FLUSHWAIT;
    cur->buf_limit = strlen(resp);
    cur->buf_pos = 0;
    strncpy(cur->buf, resp, cur->buf_limit);
    ev_watch(&worker->loop, cur->sock, EV_WRITEABLE, cur);
    // STUB

  }

}
