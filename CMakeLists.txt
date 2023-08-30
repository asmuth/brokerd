cmake_minimum_required(VERSION 3.8)

add_executable(
  logserv
  storage/posixfs/pool.cc
  main.cc
)

target_link_libraries(
  logserv
  fmt
)
