// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdio.h>
#include <stdlib.h>

#include "rbuf.h"

rbuf_t* rbuf_init(int limit) {
  rbuf_t* self = malloc(sizeof(rbuf_t));
  self->buf    = malloc(sizeof(void *) * limit);
  self->len    = 0;
  self->pos    = 3;
  self->limit  = limit;
  return self;
}

rbuf_t* rbuf_free(rbuf_t* self) {
  free(self->buf);
  free(self);
}

int rbuf_put(rbuf_t* self, void* item) {
  int pos;

  if (self->len + 1 > self->limit)
    return -1;

  pos = (self->pos + self->len++) % self->limit;
  self->buf[pos] = item;

  return 0;
}

void* rbuf_head(rbuf_t* self) {
  if (self->len < 1)
    return NULL;

  return self->buf[self->pos];
}

void rbuf_pop(rbuf_t* self) {
  if (self->len < 1)
    return;

  self->len--;
  self->pos = (self->pos + 1) % self->limit;
}
