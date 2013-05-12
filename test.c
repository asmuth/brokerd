#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>

int conn_queue[2];

typedef struct {
  int              sock;
  struct sockaddr* addr;
  socklen_t        addr_len;
} connection_t;

int main(int argc, char** argv) {
  int sock;

  struct sockaddr_in addr;
  connection_t*      conn;

  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = htonl(INADDR_ANY);
  addr.sin_port = htons(2324);

  if (pipe(conn_queue) == -1) {
    printf("create pipe failed!\n");
    return 1;
  }

  sock = socket(AF_INET, SOCK_STREAM, 0);

  if (sock == -1) {
    printf("create socket failed!\n");
    return 1;
  }

  if (bind(sock, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
    printf("bind failed!\n");
    return 1;
  }

  if (listen(sock, 1024) == -1) {
    printf("bind failed!\n");
    return 1;
  }

  while (1) {

    printf("waiting...\n");

    conn = (connection_t *) calloc(1, sizeof(connection_t));
    conn->addr_len = sizeof(conn->addr);

    conn->sock = accept(sock, conn->addr, &conn->addr_len);

    if (conn->sock == -1) {
      printf("accept failed!\n");
      continue;
    }

    printf("accepted, putting into connection queue!\n");
    write(conn_queue[1], (char *) conn, sizeof(connection_t));
  }

  printf("yeah\n");
  return 0;
}
