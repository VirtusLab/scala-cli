#!/bin/bash

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
  clear

    cat <<EOF | updateFile HelloWorld.scala
object HelloWorld {
    def main(args: Array[String]) = {
        println("Hello world from ScalaCLI")
    }
}
EOF

  # Put your stuff here
  pe "scala-cli HelloWorld.scala"

  # Wait a bit to read output of last command
  sleep 5
  clear 
      cat <<EOF | updateFile HelloWorld.sc
println("Hello world from script")
EOF

  pe "scala-cli HelloWorld.sc"

  sleep 5
  echo " "
fi