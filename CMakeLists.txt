cmake_minimum_required(VERSION 2.8)

project(fyrehose)

set(CMAKE_MODULE_PATH ${CMAKE_CURRENT_SOURCE_DIR}/cmake)

include(FindPkgConfig)
include(CheckIncludeFiles)
include(CheckFunctionExists)
include(CheckVariableExists)
include(CheckTypeSize)
include(CheckLibraryExists)
include(CheckCSourceCompiles)
include(CMakeDetermineCCompiler)

# we need the following definitions in order to get some special
# OS-level features like posix_fadvise() or readahead().
add_definitions(-DXOPEN_SOURCE=600)
add_definitions(-DGNU_SOURCE)

# enforce 64bit i/o operations, even on 32bit platforms
add_definitions(-D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -D_LARGE_FILES)

CHECK_INCLUDE_FILES(gtest/gtest.h HAVE_GTEST_GTEST_H)
CHECK_INCLUDE_FILES(sys/sendfile.h HAVE_SYS_SENDFILE_H)
CHECK_FUNCTION_EXISTS(sendfile HAVE_SENDFILE)
CHECK_FUNCTION_EXISTS(readahead HAVE_READAHEAD)
CHECK_FUNCTION_EXISTS(posix_fadvise HAVE_POSIX_FADVISE)
CHECK_INCLUDE_FILES(sys/epoll.h HAVE_SYS_EPOLL_H)
CHECK_FUNCTION_EXISTS(accept4 HAVE_ACCEPT4)

include_directories(${CMAKE_CURRENT_BINARY_DIR}/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

add_definitions(-DHAVE_SYSCONFIG_H)
configure_file(
	${CMAKE_CURRENT_SOURCE_DIR}/include/sysconfig.h.cmake
	${CMAKE_CURRENT_BINARY_DIR}/include/sysconfig.h)

add_custom_target(ctags
	COMMAND ctags -R --c++-kinds=+p --fields=+iaS --extra=+q ${CMAKE_SOURCE_DIR}/src ${CMAKE_SOURCE_DIR}/include
	WORKING_DIRECTORY ${CMAKE_SOURCE_DIR})

add_subdirectory(src)
