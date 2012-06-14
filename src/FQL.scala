package com.paulasmuth.fyrehose;

trait FQL extends QueryLexer {

  // commands
  X_COMMAND.define('stream, classOf[StreamQuery])

  // keywords
  X_KEYWORD.define('and)
  X_KEYWORD.define('or)

  // scopes
  //X_SCOPE.define('since)

  // where / where_not
  X_WHERE.define('equals_str, """ *= *'([^']*)'""")

}
