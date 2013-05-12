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
#include <string.h>

#include "worker.h"
#include "conn.h"

worker_t* worker_init() {
  int err;

  worker_t* worker = calloc(1, sizeof(worker));

  if (pipe(worker->queue) == -1) {
    printf("create pipe failed!\n");
    return NULL;
  }

  err = pthread_create(&worker->thread, NULL, worker_run, worker);

  if (err) {
    printf("error starting worker: %i\n", err);
    return NULL;
  }

  return worker;
}

void worker_stop(worker_t* worker) {
  printf("worker_stop\n");
  //pthread_join(worker->thread);
  free(worker);
}

void proc_conn(conn_t* conn) {
  int chunk, body_pos;

  while (conn->buf_pos < conn->buf_len) {
    chunk = read(conn->sock, conn->buf + conn->buf_pos, 10);

    if (chunk == 0) {
      printf("read EOF\n");
      conn_close(conn);
      return;
    }

    if (chunk < 0) {
      printf("error while reading...\n");
      conn_close(conn);
      return;
    }

    conn->buf_pos += chunk;

    body_pos = http_read(conn->http_req, conn->buf, conn->buf_pos);

    if (body_pos == -1) {
      printf("http_read() returned error\n");
      conn_close(conn);
    }

    if (body_pos > 0)
      break;
  }

  //printf("http: %i %s\n", conn->http_req->method, conn->http_req->uri);

  //printf("write...\n");

  char* resp = "HTTP/1.0 200 OK\r\nServer: fyrehose-v0.0.1\r\n\r\nfnord :)\r\n";
  write(conn->sock, resp, strlen(resp));

  //printf("close...\n");
  conn_close(conn);
}

void *worker_run(void* userdata) {
  worker_t* self = userdata;
  conn_t* conn;

  while (1) {
    //printf("worker waiting...\n");

    if (read(self->queue[0], &conn, sizeof(conn_t *)) != sizeof(conn_t *)) {
      printf("error reading from conn_queue\n");
      continue;
    }

    //printf("read next connection...\n");
    proc_conn(conn);
  }

  return self;
}
