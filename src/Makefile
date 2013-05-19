## This file is part of the "fyrehose" project
##   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
##
## Licensed under the MIT License (the "License"); you may not use this
## file except in compliance with the License. You may obtain a copy of
## the License at: http://opensource.org/licenses/MIT

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

clean:
	rm fyrehose *.o

$(TARGET): $(OBJECTS)
	$(CC) $(FLAGS) $(CFLAGS) $(DEBUGFLAGS) -o $(TARGET) $(OBJECTS)
