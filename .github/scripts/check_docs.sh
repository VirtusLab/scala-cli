#!/usr/bin/env bash

dest=$(pwd)/.scala/bin

./mill -i copyTo cli.launcher $dest/scala-cli 

export PATH=$dest:$PATH
echo Adding $dest to classpath
ls $dest

# adding --resources is a hack to get file watching for free on .md files
scala-cli docs_checker/check.scala --resources docs/cookbooks $@ -- --dest examples docs/cookbooks 