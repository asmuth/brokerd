bin_PROGRAMS = brokerd brokerctl

brokerctl_SOURCES = brokerctl.cc
brokerctl_LDADD = \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/http \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/json \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/uri \
    -ltransport_http \
    -ltransport_json \
    -ltransport_uri

brokerd_SOURCES = brokerd.cc
brokerd_LDADD = \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/http \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/json \
    -L$(abs_top_builddir)/deps/libtransport/src/libtransport/uri \
    -ltransport_http \
    -ltransport_json \
    -ltransport_uri
