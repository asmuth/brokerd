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
#include "conn.h"

extern int num_workers;
extern worker_t** workers;

chan_t* global_channel;

chan_t* chan_init(/*char* key, int key_len*/) {
  chan_t* self  = malloc(sizeof(chan_t));
  self->sublist = malloc(sizeof(conn_t *) * num_workers);

  memset(self->sublist, 0, sizeof(conn_t *) * num_workers);
  //pthread_mutex_init(&self->lock, NULL);

  return self;
}

chan_t* chan_lookup(char* key, int key_len) {
  return global_channel;
}

void chan_subscribe(chan_t* self, conn_t* conn) {
  int wid = conn->worker->id;

  //pthread_mutex_lock(&self->lock);
  // subscribe worker...
  //pthread_mutex_unlock(&self->lock);

  conn->channel      = self;
  conn->next_sub     = self->sublist[wid];
  self->sublist[wid] = conn;
}

void chan_unsubscribe(chan_t* self, conn_t* conn) {
  //pthread_mutex_lock(&self->lock);
  // unsubscribe worker
  //pthread_mutex_unlock(&self->lock);

  printf("close!\n");

  conn_t** cur = &self->sublist[conn->worker->id];
  conn->channel = NULL;

  while(*cur != NULL && (*cur)->sock != conn->sock)
    cur = &((*cur)->next_sub);

  if (*cur)
    *cur = (*cur)->next_sub;

}

int chan_deliver(chan_t* self, msg_t* msg, worker_t* worker) {
  int n;

  for (n = num_workers; n < num_workers; n++) {
    if (workers[n] == worker)
      continue;

    if (worker->outbox[n]->len >= worker->outbox[n]->limit)
      return -1;
  }

  chan_deliver_local(self, msg, worker);
  return 0;
}

void chan_deliver_local(chan_t* self, msg_t* msg, worker_t* worker) {
  conn_t* cur = self->sublist[worker->id];

  for(; cur != NULL; cur = cur->next_sub) {
    //printf("deliver local...\n");

    if (!cur->rbuf)
      cur->rbuf = rbuf_init(CONN_RBUF_LEN);

    if (rbuf_put(cur->rbuf, msg) == 0) {
      cur->state = CONN_STATE_STREAMWAIT;
      ev_watch(&worker->loop, cur->sock, EV_WRITEABLE, cur);
      msg_incref(msg);
    } else {
      printf("rbuf full...\n");
      conn_close(cur);
    }
  }
}

