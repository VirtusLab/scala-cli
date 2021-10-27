#!/bin/bash

set -e

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  echo "println(1)" | scala-cli -
  echo "// using lib \"com.lihaoyi::os-lib::0.7.8\"" | scala-cli  -
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clearConsole

  # Put your stuff here
  cat <<EOF | updateFile file
Hello World from file
EOF

  cat <<EOF | updateFile script.sc
// using lib "com.lihaoyi::os-lib::0.7.8"

val filePath = os.pwd / "file"
val fileContent = os.read(filePath)
println(fileContent)
EOF

  pe "scala-cli script.sc"

  doSleep 5

  clearConsole
  
  cat <<EOF | updateFile hello.sc
val message = "Hello from Scala script"
println(message)
EOF

  pe "scala-cli hello.sc"

  doSleep 5
  clearConsole
  
  cat <<EOF | updateFile scala-script.sc
#!/usr/bin/env scala-cli

println("Hello world")
EOF

  pe "chmod +x scala-script.sc"
  pe "./scala-script.sc"

  echo " "
  # Wait a bit to read output of last command
  doSleep 2
  echo " " && echo "ok" > status.txt
fi