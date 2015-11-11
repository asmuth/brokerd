/**
 * This file is part of the "FnordMetric" project
 *   Copyright (c) 2014 Paul Asmuth, Google Inc.
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <stdlib.h>
#include "stx/stringutil.h"
#include "stx/http/httprouter.h"
#include "stx/http/httpserver.h"
#include "stx/io/filerepository.h"
#include "stx/io/fileutil.h"
#include "stx/json/jsonrpc.h"
#include "stx/json/jsonrpchttpadapter.h"
#include "brokerd/FeedService.h"
#include "stx/test/unittest.h"

UNIT_TEST(FeedServiceTest);

TEST_CASE(FeedServiceTest, IntegrationTest, [] () {
  auto log_path = "/tmp/__fnord_logstream_service_test";
  stx::FileUtil::mkdir_p(log_path);
  stx::FileRepository repo(log_path);
  repo.deleteAllFiles();

  auto msggen = [] (int i) { return stx::StringUtil::format("msg$0", i); };

  const int kPerRun = 1000;
  const int kNumRuns = 10;
  const int kBatchSize = 23;

  auto checkall = [&] (stx::feeds::FeedService* ls, int i) {
    int n = 0;
    for (size_t offset = 0; ;) {
      auto entries = ls->fetch("teststream", offset, kBatchSize);

      if (entries.size() == 0) {
        break;
      }

      for (const auto& e : entries) {
        EXPECT_EQ(e.data, msggen(n++));
      }

      offset = entries.back().next_offset;
    }

    EXPECT_EQ(n, i);
  };

  int i = 0;
  for (int r = 0; r < kNumRuns; ++r) {
    stx::feeds::FeedService ls_service{
        stx::FileRepository(log_path)};

    if (r > 0) {
      checkall(&ls_service, i);
    }

    for (int limit = i + kPerRun; i < limit ; ++i) {
      ls_service.append("teststream", msggen(i));
    }

    checkall(&ls_service, i);
  }
});
