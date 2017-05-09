## Process this file with autoconf to produce configure.
## In general, the safest way to proceed is to run ./autogen.sh

AC_PREREQ(2.59)
AC_INIT([brokerd],[0.2.0],[authors@brokerd.org],[brokerd])
AM_INIT_AUTOMAKE([foreign subdir-objects serial-tests])
AM_MAINTAINER_MODE([enable])
AC_CONFIG_SRCDIR([configure.ac])
AM_SILENT_RULES([yes])
AC_LANG([C++])

# Check for build env, standard header files and library functions
AC_PROG_CC
AC_PROG_CXX
AC_PROG_RANLIB
AM_PROG_AS
AM_PROG_AR
AC_HEADER_STDC
AC_FUNC_MEMCMP
AC_FUNC_STRTOD
AC_CHECK_HEADERS([fcntl.h inttypes.h limits.h stdlib.h unistd.h syslog.h])
AC_CHECK_FUNCS([ftruncate posix_fallocate posix_fadvise gethostbyname_r])
AM_CONDITIONAL([HAVE_SYSLOG_H], [test x$HAVE_SYSLOG_H = x1])
AM_CONDITIONAL([HAVE_GETHOSTBYNAME_R], [test x$HAVE_GETHOSTBYNAME_R = x1])
AM_CONDITIONAL([HAVE_POSIX_FALLOCATE], [test x$HAVE_POSIX_FALLOCATE = x1])
AM_CONDITIONAL([HAVE_POSIX_FADVISE], [test x$HAVE_POSIX_FADVISE = x1])
ACX_PTHREAD
ACX_ZLIB

AC_CONFIG_SUBDIRS([deps/libtransport])

AC_CONFIG_FILES([
    Makefile
    src/brokerd/Makefile])

AC_OUTPUT
