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
#include <netinet/tcp.h>
#include <netinet/in.h>

#include "conn.h"
#include "worker.h"
#include <signal.h>


int ssock;
int running = 1;
worker_t** worker;
int num_workers;

void quit(int fnord) {
  int n;

  printf("shutdown...\n");

  running = 0;
  close(ssock);

  for (n = 0; n < num_workers; n++)
    worker_stop(worker[n]);

}

int main(int argc, char** argv) {
  struct    sockaddr_in server_addr;
  int       n, opt, port = 2323;
  conn_t*   conn;

  signal(SIGQUIT, quit);
  signal(SIGINT, quit);

  num_workers = 24;
  worker = malloc(sizeof(worker_t *) * num_workers);

  for (n = 0; n < num_workers; n++) {
    worker[n] = worker_init(n);

    if (worker[n] == NULL)
      return 1;
  }

  server_addr.sin_family = AF_INET;
  server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
  server_addr.sin_port = htons(port);

  ssock = socket(AF_INET, SOCK_STREAM, 0);

  if (ssock == -1) {
    printf("create socket failed!\n");
    return 1;
  }

  opt = 1; /* enable the following socket options */

  if (setsockopt(ssock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
    perror("setsockopt(SO_REUSEADDR)");
    return 1;
  }

  if (setsockopt(ssock, SOL_TCP, TCP_QUICKACK, &opt, sizeof(opt)) < 0) {
    perror("setsockopt(TCP_QUICKACK)");
    return 1;
  }

  if (setsockopt(ssock, SOL_TCP, TCP_DEFER_ACCEPT, &opt, sizeof(opt)) < 0) {
    perror("setsockopt(TCP_QUICKACK)");
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

  for (n = 0; running == 1; n++) {
    conn = conn_init(4096);
    conn->sock = accept(ssock, conn->addr, &conn->addr_len);

    if (conn->sock == -1) {
      printf("accept failed!\n");
      free(conn);
      continue;
    }

    conn_set_nonblock(conn);
    write(worker[n % num_workers]->queue[1], (char *) &conn, sizeof(conn_t *));
  }

  return 0;
}
