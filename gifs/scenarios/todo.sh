#!/bin/bash

########################
# include the magic
########################

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

. $SCRIPT_DIR/../demo-magic.sh

# # hide the evidence
clear

# Put your stuff here

pe "scala-cli about"
pe "echo 'println(\"TODO\")' | scala-cli -"
