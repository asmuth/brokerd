// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdio.h>

#include "msg.h"

// FIXPAUL: this shouldnt need two mallocs!
msg_t* msg_init(size_t len){
  msg_t* self = malloc(sizeof(msg_t));
  self->data = malloc(len);
  self->len  = len;
  self->refc = 1;
  return self;
}

void msg_incref(msg_t* self) {
  __sync_add_and_fetch(&self->refc, 1);
}

void msg_decref(msg_t* self) {
  if (__sync_sub_and_fetch(&self->refc, 1) != 0)
    return;

  free(self->data);
  free(self);
}
