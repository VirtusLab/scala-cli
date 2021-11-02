#!/usr/bin/env bash

set -e

dest=$(pwd)/.scala/bin

./mill -i copyTo cli.launcher $dest/scala-cli 

export PATH=$dest:$PATH
echo Adding $dest to classpath
ls $dest

if [ $# -eq 0 ]
  then
    toCheck="website/docs/cookbooks website/docs/commands"
  else
    toCheck=$@
fi

# adding --resources is a hack to get file watching for free on .md files
scala-cli sclicheck/sclicheck.scala --resources docs -- $toCheck 