SHELL        = /bin/sh
CC           = clang
FLAGS        =
CFLAGS       = -Wall -Wextra
LDFLAGS      = -lpthread
DEBUGFLAGS   = -g3 -O0 -D _DEBUG
RELEASEFLAGS = -O3 -march=native -mtune=native -D NDEBUG -combine -fwhole-program

TARGET       = fyrehose
SOURCES      = fyrehose.c ev.c conn.c http.c worker.c
HEADERS      = fyrehose.h ev.h conn.h http.h worker.h
OBJECTS      = $(SOURCES:.c=.o)

PREFIX       = $(DESTDIR)/usr/local
BINDIR       = $(PREFIX)/bin

all: $(TARGET)

$(TARGET): $(OBJECTS)
	$(CC) $(FLAGS) $(CFLAGS) $(DEBUGFLAGS) -o $(TARGET) $(OBJECTS)
