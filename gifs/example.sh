#!/bin/bash

########################
# include the magic
########################
. demo-magic.sh

# Full docs at https://github.com/paxtonhare/demo-magic

# hide the evidence
clear


# commands to record

# this will run command and record both input and output
pe "scala-cli about"
pe "scala-cli run ."
