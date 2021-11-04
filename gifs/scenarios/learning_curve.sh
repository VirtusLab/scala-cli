#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  echo "println(1)" | scala-cli -S 3.0.2 -
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsole

  # Put your stuff here
   cat <<EOF | updateFile Hello.scala
// using scala 3.0.2

@main def hello() = println("Hello world from ScalaCLI")
EOF

  pe "scala-cli Hello.scala"

  # Wait a bit to read output of last command
  doSleep 2
  clearConsole 

 cat <<EOF | updateFile Hello.scala
// using scala 2.13.6

object Hello extends App {
 println("Hello world from ScalaCLI")
}
EOF

  pe "scala-cli Hello.scala"
  echo " " && echo "ok" > status.txt
fi