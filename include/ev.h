// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#ifndef EV_H
#define EV_H

#define EV_WATCH_READ 1
#define EV_WATCH_WRITE 2

typedef struct {
  int   watch;
  int   fired;
  void* userdata;
} ev_event_t;

typedef struct {
  fd_set      op_read;
  fd_set      op_write;
  ev_event_t* events;
  int         max_fd;
  int         setsize;
} ev_state_t;

ev_state_t* ev_init();
void ev_watch(ev_state_t* state, int fd, int flags, void* userdata);
void ev_unwatch(ev_state_t* state, int fd);
int  ev_poll(ev_state_t* state);

#endif
