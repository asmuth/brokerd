#!/bin/bash

TIMEOUT=5 # connection idle timeout

die(){ 
  echo $1 && exit 1 
}

for proc in {pv,nc,sed}; do
  which $proc &> /dev/null || die "$proc not found"
done

if [[ "$#" -ne 2 ]]; then
  die "usage: $0 [source_host:port] [target_host:port]"
fi

while sleep 0.1; do

  echo "fyrehose-bridge [$1 => $2] starting"

  echo '!stream()' | \
    nc -w $TIMEOUT `echo $1 | sed -e 's/:/ /'` | \
    pv | \
    nc -w $TIMEOUT `echo $2 | sed -e 's/:/ /'`

done
