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
  clearConsole

  # Put your stuff here
    cat <<EOF | updateFile count_lines.sc
#!/usr/bin/env scala-cli
using scala 3.0.2
import scala.io.StdIn.readLine
import LazyList.continually

println(continually(readLine).takeWhile(_ != null).length)
EOF
  doSleep 2
  pe "chmod +x count_lines.sc"
  pe 'echo -e "abc\ndef" | ./count_lines.sc'
  pe 'echo -e "abc\ndef\nghi" | ./count_lines.sc'
  doSleep 4
  echo " " && echo "ok" > status.txt
fi
