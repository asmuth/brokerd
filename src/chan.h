// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <pthread.h>

#include "conn.h"
#include "msg.h"

#ifndef CHAN_H
#define CHAN_H


#define CHAN_KEYLEN 256
#define CHAN_MAXSUBSCRIBERS 256

typedef struct {
  char            key[CHAN_KEYLEN];
  conn_t**        sublist;
  pthread_mutex_t lock;
} chan_t;

chan_t* chan_init();
chan_t* chan_lookup(char* key, int key_len);
void chan_subscribe(chan_t* chan, conn_t* conn);
void chan_unsubscribe(chan_t* chan, conn_t* conn);
int chan_deliver(chan_t* self, msg_t* msg, worker_t* worker);
void chan_deliver_local(chan_t* self, msg_t* msg, worker_t* worker);

#endif
