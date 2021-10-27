#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Code here will be run before the recording session
  # Warm up scala-cli
  # echo "println(1)" | scala-cli -
  # or do other preparation (e.g. create code)
  echo OK
else
  . $SCRIPT_DIR/../demo-magic.sh
  # hide the evidence
  clearConsole

  # Put your stuff here
  # pe "echo 'println(\"<description>\")' | scala-cli -"

  # Wait a bit to read output of last command
  # doSleep 2
  echo " " && echo "ok" > status.txt && echo "ok" > status.txt
fi