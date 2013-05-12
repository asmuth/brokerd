// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include "conn.h"
#include "server.h"
#include "worker.h"
#include <signal.h>

worker_t*  worker;
int running = 1;

void quit(int fnord) {
  running = 0;
  printf("shutdown...\n");

  worker_stop(worker);
  server_stop();
}

int main(int argc, char** argv) {
  int server;
  conn_t*    conn;

  signal(SIGQUIT, quit);
  signal(SIGINT, quit);

  worker = worker_init();

  if (worker == NULL)
    return 1;

  if (server_start(2324) == -1)
    return 1;

  while (running) {
    printf("waiting...\n");
    conn = conn_init();

    conn->sock = accept(ssock, conn->addr, &conn->addr_len);

    if (conn->sock == -1) {
      printf("accept failed!\n");
      free(conn);
      continue;
    }

    printf("accepted, putting into connection queue!\n");
    write(worker->queue[1], (char *) &conn, sizeof(conn_t *));
  }

  printf("yeah\n");
  return 0;
}

