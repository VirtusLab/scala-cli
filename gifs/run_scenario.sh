#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [ "$#" != "1" ]; then
  echo "Please provide one scenario file!"
  exit 1
fi


fileName=${1##*/}
name=${fileName%%.sh}
script=$SCRIPT_DIR/scenarios/$name.sh 

ls $script

#warmup
$script -n
echo "Done with $?"

test -f status.txt && rm status.txt

#do recording
asciinema rec --overwrite --command="$script -n" $SCRIPT_DIR/out/$name.cast

test -f status.txt || (
  echo "Scenarion $sctip failed." &&
  echo "In case logs show that is should succeed check if it creates a status.txt file at the end" &&
  exit 1
)