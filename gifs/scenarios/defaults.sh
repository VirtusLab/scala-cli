#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
    echo "println(1)" | scala-cli -
    scala-cli fmt .
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsole

  # Put your stuff here
    cat <<EOF | updateFile Main.scala
@main def hello() = {println("Hello "+"world")}
EOF
  doSleep 2
  pe 'scala-cli .'
  pe 'scala-cli fmt .'
  p "cat Main.scala"
  rougify --theme tulip Main.scala
  pe 'scala-cli --power package .'
  doSleep 4
  echo " " && echo "ok" > status.txt
fi
