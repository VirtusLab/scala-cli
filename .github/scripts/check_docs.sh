#!/usr/bin/env bash

dest=$(pwd)/.scala/bin

# TODO
# ./mill -i copyTo cli.launcher $dest/scala-cli 

export PATH=$dest:$PATH
echo Adding $dest to classpath
ls $dest

if [ $# -eq 0 ]
  then
    toCheck=website/docs/cookbooks
  else
    toCheck=$@
fi

# adding --resources is a hack to get file watching for free on .md files
scala-cli docs_checker/check.scala --resources docs -- $toCheck 