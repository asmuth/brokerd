/**
 * This file is part of the "libfnord" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <stdlib.h>
#include <unistd.h>
#include <iostream>
#include <brokerd/util/flagparser.h>
#include <brokerd/util/logging.h>
#include <brokerd/util/cli.h>

void cmd_monitor(const FlagParser& flags) {
  std::cerr << "test" << std::endl;
}

//void cmd_export(const cli::FlagParser& flags) {
//  Random rnd;
//  stx::thread::EventLoop ev;
//
//  auto evloop_thread = std::thread([&ev] {
//    ev.run();
//  });
//
//  http::HTTPConnectionPool http(&ev);
//  BrokerClient broker(&http);
//
//  Duration poll_interval(0.5 * kMicrosPerSecond);
//  uint64_t maxsize = 4 * 1024 * 1024;
//  size_t batchsize = 1024;
//  auto topic = flags.getString("topic");
//  String prefix = StringUtil::stripShell(topic);
//  auto path = flags.getString("datadir");
//  auto cursorfile_path = FileUtil::joinPaths(path, prefix + ".cur");
//
//  Vector<InetAddr> servers;
//  for (const auto& s : flags.getStrings("server")) {
//    servers.emplace_back(InetAddr::resolve(s));
//  }
//
//  if (servers.size() == 0) {
//    RAISE(kUsageError, "no servers specified");
//  }
//
//  stx::logInfo("brokerctl", "Exporting topic '$0'", topic);
//
//  ExportCursor cursor;
//  if (FileUtil::exists(cursorfile_path)) {
//    auto buf = FileUtil::read(cursorfile_path);
//    msg::decode<ExportCursor>(buf.data(), buf.size(), &cursor);
//
//    auto cur_topic = cursor.topic_cursor().topic();
//    if (cur_topic != topic) {
//      RAISEF(kRuntimeError, "topic mismatch: '$0' vs '$1;", cur_topic, topic);
//    }
//
//    stx::logInfo(
//        "brokerctl",
//        "Resuming export from sequence $0",
//        cursor.head_sequence());
//  } else {
//    cursor.mutable_topic_cursor()->set_topic(topic);
//    stx::logInfo("brokerctl", "Starting new export from epoch...");
//  }
//
//  Vector<String> rows;
//  size_t rows_size = 0;
//  for (;;) {
//    size_t n = 0;
//    for (const auto& server : servers) {
//      auto msgs = broker.fetchNext(
//          server,
//          cursor.mutable_topic_cursor(),
//          batchsize);
//
//      for (const auto& msg : msgs.messages()) {
//        rows.emplace_back(msg.data());
//        rows_size += msg.data().size();
//      }
//
//      n += msgs.messages().size();
//    }
//
//    if (rows_size >= maxsize) {
//      auto next_seq = cursor.head_sequence() + 1;
//      stx::logInfo("brokerctl", "Writing sequence $0", next_seq);
//
//      auto dstpath = FileUtil::joinPaths(
//          path,
//          StringUtil::format("$0.$1.$2", prefix, next_seq, "json"));
//
//      {
//        auto tmpfile = File::openFile(
//            dstpath + "~",
//            File::O_WRITE | File::O_CREATEOROPEN | File::O_TRUNCATE);
//
//        tmpfile.write("[");
//        for (int i = 0; i < rows.size(); ++i) {
//          if (i > 0) { tmpfile.write(", "); }
//          tmpfile.write(rows[i]);
//        }
//        tmpfile.write("]\n");
//      }
//
//      cursor.set_head_sequence(next_seq);
//      FileUtil::mv(dstpath + "~", dstpath);
//      rows.clear();
//      rows_size = 0;
//
//      {
//        auto cursorfile = File::openFile(
//            cursorfile_path + "~",
//            File::O_WRITE | File::O_CREATEOROPEN | File::O_TRUNCATE);
//
//        cursorfile.write(*msg::encode(cursor));
//      }
//
//      FileUtil::mv(cursorfile_path + "~", cursorfile_path);
//    }
//
//    if (n == 0) {
//      usleep(poll_interval.microseconds());
//    }
//  }
//
//  evloop_thread.join();
//}

int main(int argc, const char** argv) {
  FlagParser flags;

  flags.defineFlag(
      "help",
      FlagParser::T_STRING,
      false,
      "?",
      NULL);

  flags.defineFlag(
      "version",
      FlagParser::T_SWITCH,
      false,
      "V",
      NULL);

  flags.defineFlag(
      "verbose",
      FlagParser::T_SWITCH,
      false,
      "v",
      NULL);

  /* parse flags */
  {
    auto rc = flags.parseArgv(argc, argv);
    if (!rc.isSuccess()) {
      std::cerr << "ERROR: " << rc.getMessage() << std::endl;
      return 1;
    }
  }

  bool verbose = flags.isSet("verbose");
  auto cmd_argv = flags.getArgv();
  Logger::logToStderrWithoutDecoration();
  if (verbose) {
    Logger::get()->setMinimumLogLevel(strToLogLevel("DEBUG"));
  } else {
    Logger::get()->setMinimumLogLevel(strToLogLevel("WARNING"));
  }

  /* print help */
  if (flags.isSet("version")) {
    std::cerr <<
        StringUtil::format(
            "brokerd $0\n"
            "Copyright (c) 2016, Paul Asmuth et al. All rights reserved.\n\n",
            BROKERD_VERSION);

    return 0;
  }

  if (flags.isSet("help")) {
    std::cerr <<
        "Usage: $ brokerctl <command> [OPTIONS]\n"
        "   -v, --verbose             Run in verbose mode\n"
        "   -?, --help                Display this help text and exit\n"
        "   -V, --version             Display the version of this binary and exit\n"
        "\n"
        "Examples:\n"
        "   $ brokerctl tail mytopic\n";

    return 0;
  }

  CLI cli;

  /* command: monitor */
  auto monitor_cmd = cli.defineCommand("monitor");
  monitor_cmd->onCall(std::bind(&cmd_monitor, std::placeholders::_1));

  cli.call(flags.getArgv());
  return 0;
}

