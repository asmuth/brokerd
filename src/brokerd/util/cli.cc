/**
 * This file is part of the "libstx" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * libstx is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#include <iostream>
#include "cli.h"

RefPtr<CLICommand> CLI::defineCommand(const String& command) {
  RefPtr<CLICommand> cmd(new CLICommand(command));
  commands_.emplace(command, cmd);
  return cmd;
}

void CLI::call(const std::vector<std::string>& argv) {
  if (argv.size() == 0) {
    std::cerr << "error: no command" << std::endl;
    return;
  }

  auto cmd_name = argv[0];
  auto cmd_argv = argv;
  cmd_argv.erase(cmd_argv.begin());

  auto cmd_iter = commands_.find(cmd_name);
  if (cmd_iter == commands_.end()) {
    std::cerr << "command not found: " << cmd_name << std::endl;
    return;
  }

  auto& cmd = cmd_iter->second;
  cmd->call(cmd_argv);
}

