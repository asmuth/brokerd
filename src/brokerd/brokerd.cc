/**
 * This file is part of the "brokerd" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * FnordMetric is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <stdlib.h>
#include <unistd.h>
#include <cstring>
#include <iostream>
#include <regex>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/file.h>
#include <brokerd/util/flagparser.h>
#include <brokerd/util/logging.h>
#include <brokerd/util/daemonize.h>
#include <brokerd/http_server.h>

bool parseListenAddr(
    const std::string& addr,
    std::string* host,
    uint16_t* port) {
  std::smatch m;
  std::regex listen_regex("([0-9a-zA-Z-_.]+):([0-9]+)");
  if (std::regex_match(addr, m, listen_regex)) {
    *host = m[1];
    *port = std::stoul(m[2]);
    return true;
  } else {
    return false;
  }
}

int main(int argc, const char** argv) {
  FlagParser flags;

  flags.defineFlag(
      "listen_http",
      FlagParser::T_STRING,
      true,
      NULL,
      NULL);

  flags.defineFlag(
      "datadir",
      FlagParser::T_STRING,
      true,
      NULL,
      NULL);

  flags.defineFlag(
      "disklimit",
      FlagParser::T_STRING,
      false,
      NULL,
      NULL);

  flags.defineFlag(
      "disklimit_disk",
      FlagParser::T_STRING,
      false,
      NULL,
      NULL);

  flags.defineFlag(
      "loglevel",
      FlagParser::T_STRING,
      false,
      NULL,
      "INFO");

  flags.defineFlag(
      "daemonize",
      FlagParser::T_SWITCH,
      false,
      NULL,
      NULL);

  flags.defineFlag(
      "pidfile",
      FlagParser::T_STRING,
      false,
      NULL,
      NULL);

  flags.defineFlag(
      "log_to_syslog",
      FlagParser::T_SWITCH,
      false,
      NULL,
      NULL);

  flags.defineFlag(
      "nolog_to_stderr",
      FlagParser::T_SWITCH,
      false,
      NULL,
      NULL);

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

  /* parse flags */
  {
    auto rc = flags.parseArgv(argc, argv);
    if (!rc.isSuccess()) {
      std::cerr << "ERROR: " << rc.getMessage() << std::endl;
      return 1;
    }
  }

  /* setup logging */
  if (!flags.isSet("nolog_to_stderr") && !flags.isSet("daemonize")) {
    Logger::logToStderr("brokerd");
  }

  if (flags.isSet("log_to_syslog")) {
    Logger::logToSyslog("brokerd");
  }

  Logger::get()->setMinimumLogLevel(strToLogLevel(flags.getString("loglevel")));

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
        "Usage: $ brokerd [OPTIONS]\n"
        "   --listen_http <addr>          Listen for HTTP connection on this address\n"
        "   --datadir <dir>               Set the data directory\n"
        "   --disklimit <limit>           Delete old messages to keep total size < limit\n"
        "   --disklimit_channel <limit>   Delete old messages to keep every channel size < limit\n"
        "   --daemonize                   Daemonize the server\n"
        "   --pidfile <file>              Write a PID file\n"
        "   --loglevel <level>            Minimum log level (default: INFO)\n"
        "   --[no]log_to_syslog           Do[n't] log to syslog\n"
        "   --[no]log_to_stderr           Do[n't] log to stderr\n"
        "   -?, --help                    Display this help text and exit\n"
        "   -V, --version                 Display the version of this binary and exit\n"
        "\n"
        "Examples:\n"
        "   $ brokerd --datadir /var/brokerd --listen_http localhost:8080\n"
        "   $ brokerd --datadir /var/brokerd --listen_http localhost:8080 --disklimit 20GB\n";

    return 0;
  }

  /* daemonize */
  auto rc = ReturnCode::success();
  if (rc.isSuccess() && flags.isSet("daemonize")) {
    rc = daemonize();
  }

  /* write pidfile */
  int pidfile_fd = -1;
  auto pidfile_path = flags.getString("pidfile");
  if (rc.isSuccess() && !pidfile_path.empty()) {
    pidfile_fd = open(
        pidfile_path.c_str(),
        O_WRONLY | O_CREAT,
        0666);

    if (pidfile_fd < 0) {
      rc = ReturnCode::errorf(
          "IO_ERROR",
          "writing pidfile failed: $0",
          std::strerror(errno));
    }

    if (rc.isSuccess() && flock(pidfile_fd, LOCK_EX | LOCK_NB) != 0) {
      close(pidfile_fd);
      pidfile_fd = -1;
      rc = ReturnCode::error("IO_ERROR", "locking pidfile failed");
    }

    if (rc.isSuccess() && ftruncate(pidfile_fd, 0) != 0) {
      close(pidfile_fd);
      pidfile_fd = -1;
      rc = ReturnCode::error("IO_ERROR", "writing pidfile failed");
    }

    auto pid = StringUtil::toString(getpid());
    if (rc.isSuccess() &&
        write(pidfile_fd, pid.data(), pid.size()) != pid.size()) {
      close(pidfile_fd);
      pidfile_fd = -1;
      rc = ReturnCode::error("IO_ERROR", "writing pidfile failed");
    }
  }

  /* start http server */
  std::unique_ptr<brokerd::HTTPServer> http_server;
  if (rc.isSuccess()) {
    std::string http_bind;
    uint16_t http_port;
    auto parse_rc = parseListenAddr(
        flags.getString("listen_http"),
        &http_bind,
        &http_port);

    if (parse_rc) {
      http_server.reset(new brokerd::HTTPServer(nullptr));
      rc = http_server->listenAndRun(http_bind, http_port);
    } else {
      rc = ReturnCode::error("ERUNTIME", "invalid value for --listen_http");
    }
  }

  /* unlock pidfile */
  if (pidfile_fd > 0) {
    unlink(pidfile_path.c_str());
    flock(pidfile_fd, LOCK_UN);
    close(pidfile_fd);
  }

  return rc.isSuccess() ? 0 : 1;
}

