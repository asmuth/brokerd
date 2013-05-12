#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include "test.h"

int conn_queue[2];

typedef struct {
  int              sock;
  struct sockaddr* addr;
  socklen_t        addr_len;
} connection_t;

int main(int argc, char** argv) {
  int server;
  connection_t* conn;
  pthread_t     worker;

  if (start_worker(&worker) == -1)
    return 1;

  server = start_server(2324);

  if (server == -1)
    return 1;

  while (1) {
    printf("waiting...\n");

    conn = (connection_t *) calloc(1, sizeof(connection_t));
    conn->addr_len = sizeof(conn->addr);

    conn->sock = accept(server, conn->addr, &conn->addr_len);

    if (conn->sock == -1) {
      printf("accept failed!\n");
      continue;
    }

    printf("accepted, putting into connection queue!\n");
    write(conn_queue[1], (char *) &conn, sizeof(connection_t *));
  }

  printf("yeah\n");
  return 0;
}

void *work(void* fnord) {
  connection_t* conn;

  while (1) {
    if (read(conn_queue[0], &conn, sizeof(connection_t *)) != sizeof(connection_t *)) {
      printf("error reading from conn_queue\n");
      continue;
    }

    printf("read next connection...\n");

    printf("write...\n");
    write(conn->sock, "fnord\n", 6);

    printf("close...\n");
    close(conn->sock);

    free(conn);
  }

  return fnord;
}

int start_server(int port) {
  int sock;
  struct sockaddr_in addr;

  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = htonl(INADDR_ANY);
  addr.sin_port = htons(port);

  if (pipe(conn_queue) == -1) {
    printf("create pipe failed!\n");
    return -1;
  }

  sock = socket(AF_INET, SOCK_STREAM, 0);

  if (sock == -1) {
    printf("create socket failed!\n");
    return -1;
  }

  if (bind(sock, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
    printf("bind failed!\n");
    return -1;
  }

  if (listen(sock, 1024) == -1) {
    printf("bind failed!\n");
    return -1;
  }

  return sock;
}

int start_worker(pthread_t* thread) {
  int err;

  err = pthread_create(thread, NULL, work, NULL);

  if (err) {
    printf("error starting worker: %i\n", err);
    return -1;
  }

  return 0;
}
