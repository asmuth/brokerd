// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdlib.h>
#include <sys/select.h>

#include "ev.h"

ev_state_t* ev_init() {
  ev_state_t* state = malloc(sizeof(ev_state_t));

  FD_ZERO(&state->op_read);
  FD_ZERO(&state->op_write);

  return state;
}

void ev_watch(ev_state_t* state, conn_t* conn, int flags) {
  ev_watch_fd(state, conn->sock, flags);
}

void ev_unwatch(ev_state_t* state, conn_t* conn, int flags) {
  ev_unwatch_fd(state, conn->sock, flags);
}

void ev_watch_fd(ev_state_t* state, int fd, int flags) {
  if (flags & EV_WATCH_READ) FD_SET(fd, &state->op_read);
  if (flags & EV_WATCH_WRITE) FD_SET(fd, &state->op_write);
}

void ev_unwatch_fd(ev_state_t* state, int fd, int flags) {
  if (flags & EV_WATCH_READ) FD_CLR(fd, &state->op_read);
  if (flags & EV_WATCH_WRITE) FD_CLR(fd, &state->op_write);
}
