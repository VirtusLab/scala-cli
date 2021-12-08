#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  echo "println(1)" | scala-cli -S 3.0.2 -
  echo "println(1)" | scala-cli -S 2.13.6 -
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsole

  # Put your stuff here
  cat <<EOF | updateFile Enum.scala
object Enum extends App {
  enum Color:
    case Red, Green, Blue
    
  val values = Color.values.mkString(",")
  println(values)
}
EOF

  pe "scala-cli Enum.scala -S 3.0.2"

  doSleep 2

  pe "scala-cli Enum.scala -S 2.13.6" || true
  

  # Wait a bit to read output of last command
  doSleep 5
  echo " " && echo "ok" > status.txt
fi