#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  rm /usr/bin/scala-cli || true # remove scala-cliu from PATH
  rm /usr/bin/java || true # remove java from PATH
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsole

  # Put your stuff here
  pe scala-cli || true
  
  pe java || true

  doSleep 2
  
  pe "curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh"
  pe 'source ~/.profile'
  pe "echo 'println(\"Hello from scala-cli\")' | scala-cli -"


  # Wait a bit to read output of last command
  doSleep 2
  echo " " && echo "ok" > status.txt
fi