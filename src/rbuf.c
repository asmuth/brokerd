// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdio.h>

#include "rbuf.h"

rbuf_t* rbuf_init(int limit) {
  rbuf_t* self = malloc(sizeof(rbuf_t));
  self->buf    = malloc(sizeof(void *) * limit);
  self->len    = 0;
  self->pos    = 0;
  self->limit  = limit;
  return self;
}

rbuf_t* rbuf_free(rbuf_t* self) {
  free(self->buf);
  free(self);
}

int rbuf_put(rbuf_t* self, void* item) {
  if (self->len + 1 > self->limit)
    return -1;

  self->len++;
  self->pos = (self->pos + 1) % self->limit;

  printf("write to pos: %i -- %p\n", self->pos, item);

  self->buf[self->pos] = item;
  return 0;
}

