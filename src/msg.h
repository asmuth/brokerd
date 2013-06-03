// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT


#ifndef MSG_H
#define MSG_H

typedef struct chan_s* chan_p;

typedef struct msg_s {
  chan_p  channel;
  int     refc;
  char*   data;
  size_t  len;
} msg_t;

msg_t* msg_init();
void msg_incref(msg_t* self);
void msg_decref(msg_t* self);

#endif
