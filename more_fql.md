
GROUP keywords WHERE channel = 'dawanda.search' SPLITCOPY query WITH ' ' TO keywords SORT_BY count DESC LIMIT 6000 SINCE -12h UNTIL now

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


### Pre-Modifiers

  SPLITCOPY [src_key] WITH [pattern] TO [dst_key]


### Post-Modifiers

  LIMIT [limit]

  SORT_BY [key] DESC/ASC
  SORT_BY time NEWEST_FIRST/OLDEST_FIRST


