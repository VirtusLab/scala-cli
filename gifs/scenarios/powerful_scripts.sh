#!/bin/bash

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
    echo 'using lib "com.lihaoyi::os-lib:0.7.8"' | scala-cli -
    echo 'using lib "com.lihaoyi::pprint:0.6.6"' | scala-cli -
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clear

  # Put your stuff here
  cat <<EOF | updateFile stat.sc
using lib "com.lihaoyi::os-lib:0.7.8"
using lib "com.lihaoyi::pprint:0.6.6"
import pprint._
import os._

val path = Path(args(0), pwd)
pprintln(os.stat(path))

EOF
  sleep 3
  pe "chmod +x stat.sc"
  pe 'echo "Hello" > my_file'
  pe "scala-cli ./stat.sc -- my_file"
  # Wait a bit to read output of last command
  sleep 4
  echo " "
fi
