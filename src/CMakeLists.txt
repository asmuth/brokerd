add_definitions(-pthread)

add_executable(fyrehose fyrehose.c conn.c http.c worker.c)
target_link_libraries(fyrehose pthread)
install(TARGETS fyrehose RUNTIME DESTINATION sbin)
