#!/usr/bin/env bash

set -euxo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [ "$#" != "1" ]; then
  echo "Please provide one scenario file!"
  exit 1
fi


fileName=$(basename $1)
name=${fileName%%.sh}
script=$SCRIPT_DIR/scenarios/$name.sh 

ls $script

#warmup
$script -n
echo "Done with $?"

test -f status.txt && rm status.txt

#do recording
( # do recording with tty
  tty &&
  asciinema rec --overwrite --command="$script -n" $SCRIPT_DIR/out/$name.cast
) || ( # without tty just run the command 
  export ASCIINEMA_REC=true &&
  # remove magic from demo...
  cp $SCRIPT_DIR/demo-no-magic.sh $SCRIPT_DIR/demo-magic.sh &&
  $script -n
)

test -f status.txt || (
  echo "Scenario $script failed." &&
  echo "In case logs show that is should succeed check if it creates a status.txt file at the end" &&
  exit 1
)