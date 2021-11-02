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
    echo "println(1)" | scala-cli --native -S 2.13.6 -
   
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
object Native extends App {
  import scala.scalanative.posix.limits
  println(s"Max path length in this OS is \${limits.PATH_MAX}")
}
EOF

  pe "# Scala Native works only with Scala 2.x so far"
  pe "scala-cli --native -S 2.13.6 native.scala"
  doSleep 3

  echo " " && echo "ok" > status.txt
fi
