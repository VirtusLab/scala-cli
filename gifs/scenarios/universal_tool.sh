#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  echo "println(1)" | scala-cli -
  echo "println(1)" | scala-cli --js - &&
    echo "println(1)" | scala-cli --native -
   
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsole

  cat <<EOF | updateFile js.scala
@main def jsMain =
  import scala.scalajs.js.Dynamic.global
  println(s"Node js version: \${global.process.version}")
EOF

  pe "scala-cli --js js.scala"
  doSleep 3
  clearConsole

  cat <<EOF | updateFile native.scala
import scala.scalanative.posix.limits
@main def native() = println(s"Max path length in this OS is \${limits.PATH_MAX}")
EOF

  pe "scala-cli --native native.scala"
  doSleep 3

  echo " " && echo "ok" > status.txt
fi
