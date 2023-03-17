#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
    echo '//> using dep "com.lihaoyi::os-lib:0.9.1"' | scala-cli -
    echo '//> using dep "com.lihaoyi::pprint:0.8.1"' | scala-cli -
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsole

  # Put your stuff here
  cat <<EOF | updateFile stat.sc
//> using dep "com.lihaoyi::os-lib:0.9.1"
//> using dep "com.lihaoyi::pprint:0.8.1"
import pprint._
import os._

val path = Path(args(0), pwd)
pprintln(os.stat(path))

EOF
  doSleep 3
  pe "chmod +x stat.sc"
  pe 'echo "Hello" > my_file'
  pe "scala-cli ./stat.sc -- my_file"
  # Wait a bit to read output of last command
  doSleep 4
  echo " " && echo "ok" > status.txt
fi
