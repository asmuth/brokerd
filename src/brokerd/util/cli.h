/**
 * This file is part of the "libstx" project
 *   Copyright (c) 2015 Paul Asmuth
 *
 * libstx is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License v3.0. You should have received a
 * copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
#pragma once
#include <string>
#include <vector>
#include "exception.h"
#include "cli_command.h"

class CLI {
public:

  RefPtr<CLICommand> defineCommand(const String& command);

  /**
   * Call with an an argv array. This may throw an exception.
   */
  void call(const std::vector<std::string>& argv);

protected:
  HashMap<String, RefPtr<CLICommand>> commands_;
};

