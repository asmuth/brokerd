// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/select.h>

#include "ev.h"

ev_state_t* ev_init() {
  ev_state_t* state = malloc(sizeof(ev_state_t));

  state->setsize = FD_SETSIZE;
  state->max_fd = FD_SETSIZE - 1;

  state->events = malloc(sizeof(ev_event_t) * state->setsize);
  memset(state->events, 0, sizeof(ev_event_t) * state->setsize);

  FD_ZERO(&state->op_read);
  FD_ZERO(&state->op_write);

  return state;
}

void ev_watch(ev_state_t* state, int fd, int flags, void* userdata) {
  if (fd >= FD_SETSIZE)
    return;

  state->events[fd].userdata = userdata;

  if (flags & EV_WATCH_READ) FD_SET(fd, &state->op_read);
  if (flags & EV_WATCH_WRITE) FD_SET(fd, &state->op_write);
}

void ev_unwatch(ev_state_t* state, int fd) {
  state->events[fd].fired = 0;
  FD_CLR(fd, &state->op_read);
  FD_CLR(fd, &state->op_write);
}

int ev_poll(ev_state_t* state) {
  int res, fd;

  res = select(state->max_fd, &state->op_read, &state->op_write, NULL, NULL);

  if (res == 0) {
    printf("timeout while selecting\n");
    return -1;
  }

  if (res == -1) {
    perror("error while selecting");
    return -1;
  }

  for (fd = 0; fd <= state->max_fd; fd++) {
    state->events[fd].fired = 0;

    if (FD_ISSET(fd, &state->op_read))
      state->events[fd].fired |= EV_WATCH_READ;

    if (FD_ISSET(fd, &state->op_write))
      state->events[fd].fired |= EV_WATCH_WRITE;

  }

  return 0;
}
