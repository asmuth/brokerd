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
#include "worker.h"
#include <signal.h>

worker_t*  worker;

int ssock;
int running = 1;

void quit(int fnord) {
  running = 0;
  printf("shutdown...\n");

  worker_stop(worker);

  close(ssock);
}

int main(int argc, char** argv) {
  conn_t*            conn;
  struct sockaddr_in server_addr;
  int                server;
  int                port = 2323;

  signal(SIGQUIT, quit);
  signal(SIGINT, quit);

  worker = worker_init();

  server_addr.sin_family = AF_INET;
  server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
  server_addr.sin_port = htons(port);

  ssock = socket(AF_INET, SOCK_STREAM, 0);

  if (ssock == -1) {
    printf("create socket failed!\n");
    return 1;
  }

  if (bind(ssock, (struct sockaddr *) &server_addr, sizeof(server_addr)) == -1) {
    printf("bind failed!\n");
    return 1;
  }

  if (listen(ssock, 1024) == -1) {
    printf("bind failed!\n");
    return 1;
  }

  if (worker == NULL)
    return 1;

  while (running) {
    //printf("waiting...\n");
    conn = conn_init(4096);

    conn->sock = accept(ssock, conn->addr, &conn->addr_len);

    if (conn->sock == -1) {
      printf("accept failed!\n");
      free(conn);
      continue;
    }

    //printf("accepted, putting into connection queue!\n");
    //conn_set_nonblock(conn);
    write(worker->queue[1], (char *) &conn, sizeof(conn_t *));
  }

  printf("yeah\n");
  return 0;
}
