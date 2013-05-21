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
#include <getopt.h>

#include "conn.h"
#include "worker.h"
#include "chan.h"
#include <signal.h>

// TODO
//  - read post body from conn->msg chunk-wise
//  - worker-worker rbufs + delivery via pipe
//  - msg malloc impl. cleanup
//  - msg pipe/tee implementation
//  - ev_epoll backend
//  - free all msgs on conn close
//  - chan_lookup + thread lokal hashmap cache
//  - close conns in WAIT state on shutdown
//  - batch delivery in worker_flush_outbox
//  - inline lots of structs...

int ssock, running = 1, num_workers = 1;
worker_t** workers;

extern chan_t* global_channel;

void quit(int n) {
  if (!running)
    return;

  printf("shutdown...\n");

  running = 0;
  close(ssock);
}

int main(int argc, char** argv) {
  struct    sockaddr_in server_addr;
  int       n, opt, port = 2323, fd, queue;

  while ((opt = getopt(argc, argv, "t:p:vh?")) != -1) {
    switch (opt) {
      case 't':
        num_workers = atoi(optarg);
        break;
      case 'p':
        port = atoi(optarg);
        break;
      case 'v':
        printf("fyrehose, version 0.0.1\n");
        return 0;
      case '?':
      case 'h':
      default:
        printf("fyrehose [-p PORT] [-t THREADS]\n");
        return 0;
    }
  }

  signal(SIGQUIT, quit);
  signal(SIGINT, quit);
  signal(SIGPIPE, SIG_IGN);

  // TMP
  global_channel = chan_init();
  // TMP

  workers = malloc(sizeof(worker_t *) * num_workers);

  for (n = 0; n < num_workers; n++) {
    workers[n] = worker_init(n);

    if (workers[n] == NULL)
      return 1;
  }

  for (n = 0; n < num_workers; n++)
    worker_start(workers[n]);

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

#ifdef SOL_TCP
  if (setsockopt(ssock, SOL_TCP, TCP_QUICKACK, &opt, sizeof(opt)) < 0) {
    perror("setsockopt(TCP_QUICKACK)");
    return 1;
  }

  if (setsockopt(ssock, SOL_TCP, TCP_DEFER_ACCEPT, &opt, sizeof(opt)) < 0) {
    perror("setsockopt(TCP_QUICKACK)");
    return 1;
  }
#endif

  if (bind(ssock, (struct sockaddr *) &server_addr, sizeof(server_addr)) == -1) {
    perror("bind failed");
    return 1;
  }

  if (listen(ssock, 1024) == -1) {
    perror("listen failed");
    return 1;
  }

  for (n = 0; running == 1; n++) {
    fd = accept(ssock, NULL, NULL);

    if (!running)
      break;

    if (fd == -1) {
      perror("accept failed");
      continue;
    }

    queue = workers[n % num_workers]->conn_queue[1];

    if (write(queue, (char *) &fd, sizeof(fd)) != sizeof(fd))
      printf("error writing to work queue\n");
  }

  for (n = 0; n < num_workers; n++)
    worker_stop(workers[n]);

  free(workers);
  chan_free(global_channel);

  return 0;
}
