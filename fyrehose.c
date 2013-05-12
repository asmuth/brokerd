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

int main(int argc, char** argv) {
  int server;
  conn_t*    conn;
  pthread_t  worker;

  if (worker_init(&worker) == -1)
    return 1;

  if (server_start(2324) == -1)
    return 1;

  while (1) {
    printf("waiting...\n");

    conn = (conn_t *) calloc(1, sizeof(conn_t));
    conn->addr_len = sizeof(conn->addr);

    conn->sock = accept(ssock, conn->addr, &conn->addr_len);

    if (conn->sock == -1) {
      printf("accept failed!\n");
      continue;
    }

    printf("accepted, putting into connection queue!\n");
    write(conn_queue[1], (char *) &conn, sizeof(conn_t *));
  }

  printf("yeah\n");
  server_stop();

  return 0;
}
