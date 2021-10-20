#!/bin/bash

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

if [[ -z "${ASCIINEMA_REC}" ]]; then
  # Warm up scala-cli
  apt-get purge -y scala-cli
  # or do other preparation (e.g. create code)
else
  . $SCRIPT_DIR/../demo-magic.sh
  # # hide the evidence
  clear

  # Put your stuff here
  p scala-cli
  scala-cli
  p java
  java

  sleep 2
  
  pe "curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh"
  pe 'source ~/.profile'
  pe "echo 'println(\"Hello from scala-cli\")' | scala-cli -"


  # Wait a bit to read output of last command
  sleep 2
  echo " "
fi