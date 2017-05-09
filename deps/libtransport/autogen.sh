#!/bin/sh

# Run this script to generate the configure script and other files that will
# be included in the distribution.  These files are not checked in because they
# are automatically generated.

set -e

# Check that we're being run from the right directory.
if test ! -d src/libtransport; then
  cat >&2 << __EOF__
Could not find source code.  Make sure you are running this script from the
root of the distribution tree.
__EOF__
  exit 1
fi

set -ex

autoreconf -fi -Wall,no-obsolete

rm -rf autom4te.cache config.h.in~
exit 0
