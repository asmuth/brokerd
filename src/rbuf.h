// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#ifndef RBUF_H
#define RBUF_H

typedef struct {
  void** buf;
  int    limit;
  int    pos;
  int    len;
} rbuf_t;

rbuf_t* rbuf_init(int limit);
rbuf_t* rbuf_free(rbuf_t* self);
int rbuf_put(rbuf_t* self, void* item);

#endif
