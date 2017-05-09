bin_PROGRAMS = brokerd brokerctl

BROKERD_COMMON_SRC = \
    brokerctl.cc \
    BrokerCursor.h \
    brokerd.cc \
    BrokerServlet.cc \
    BrokerServlet.h \
    FeedEntry.h \
    FeedService.cc \
    FeedService.h \
    LocalFeed.cc \
    LocalFeed.h \
    util/autoref.cc \
    util/autoref.h \
    util/autoref_impl.h \
    util/buffer.cc \
    util/buffer.h \
    util/bufferutil.cc \
    util/bufferutil.h \
    util/cli.cc \
    util/cli.h \
    util/cli_command.cc \
    util/cli_command.h \
    util/daemonize.cc \
    util/daemonize.h \
    util/exception.cc \
    util/exception.h \
    util/file.cc \
    util/file.h \
    util/fileutil.cc \
    util/fileutil.h \
    util/flagparser.cc \
    util/flagparser.h \
    util/hash.h \
    util/ieee754.cc \
    util/ieee754.h \
    util/inputstream.cc \
    util/inputstream.h \
    util/inspect.cc \
    util/inspect.h \
    util/inspect_impl.h \
    util/logging.cc \
    util/logging.h \
    util/option.h \
    util/option_impl.h \
    util/outputstream.cc \
    util/outputstream.h \
    util/return_code.h \
    util/sha1.cc \
    util/sha1.h \
    util/status.cc \
    util/status.h \
    util/stdtypes.h \
    util/stringutil.cc \
    util/stringutil.h \
    util/stringutil_impl.h \
    util/time.cc \
    util/time.h \
    util/time_impl.h

brokerd_SOURCES = \
    $(BROKERD_COMMON_SRC) \
    brokerd.cc

brokerd_LDADD = \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/http \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/json \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/uri \
    -ltransport_http \
    -ltransport_json \
    -ltransport_uri

brokerctl_SOURCES = \
    $(BROKERD_COMMON_SRC) \
    brokerctl.cc

brokerctl_LDADD = \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/http \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/json \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/uri \
    -ltransport_http \
    -ltransport_json \
    -ltransport_uri
