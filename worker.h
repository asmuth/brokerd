// This file is part of the "fyrehose" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

#ifndef WORKER_H
#define WORKER_H

#include <pthread.h>

int worker_init(pthread_t* thread);
void* work(void* fnord);

#endif
