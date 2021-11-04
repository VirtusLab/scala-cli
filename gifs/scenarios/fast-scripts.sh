#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  echo "println(1)" | scala-cli -
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsole

  # Put your stuff here
  pe "echo 'println(\"TODO: turn gifs/scenarios/fast-scripts.sh into proper scenario showing Fast Scripts" key="fast-scripts" scripting="true\")' | scala-cli -"

  # Wait a bit to read output of last command
  doSleep 2
  echo " " && echo "ok" > status.txt
fi