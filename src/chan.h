// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#ifndef CHAN_H
#define CHAN_H

#define CHAN_KEYLEN 256

typedef struct {
  char key[CHAN_KEYLEN];
} chan_t;

chan_t* chan_init();
chan_t* chan_lookup(char* key, int key_len);

#endif
