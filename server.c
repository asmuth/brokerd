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

#include "server.h"
#include "conn.h"

int server_start(int port) {
  struct sockaddr_in addr;

  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = htonl(INADDR_ANY);
  addr.sin_port = htons(port);

  ssock = socket(AF_INET, SOCK_STREAM, 0);

  if (ssock == -1) {
    printf("create socket failed!\n");
    return -1;
  }

  if (bind(ssock, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
    printf("bind failed!\n");
    return -1;
  }

  if (listen(ssock, 1024) == -1) {
    printf("bind failed!\n");
    return -1;
  }

  return ssock;
}

void server_stop() {
  close(ssock);
}

