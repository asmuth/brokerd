
GROUP keywords WHERE channel = 'dawanda.search' SPLITCOPY query WITH ' ' TO keywords LIMIT TAIL 6000 SINCE -12h UNTIL now

stream where channel = 'dawanda.search' and num_results = 0 only(query)






### Queries

  STREAM
  COUNT
  GROUP [key]


### Filter

  WHERE [expression]
  WHERE_NOT [expression]

  WHERE [key] = [value]
  WHERE [key] EXISTS
  WHERE [key] INCLUDES [value1], [value2]...
  WHERE [key] > [value]
  WHERE [key] < [value]
  WHERE [key] % [value]


### Time Range

  SINCE [time]
  UNTIL [time]


### Modifiers

  SPLITCOPY [src_key] WITH [pattern] TO [dst_key]


### Options for: GROUP

  LIMIT HEAD [limit]
  LIMIT TAIL [limit]



