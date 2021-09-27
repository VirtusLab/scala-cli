#!/usr/bin/env bash

dest=$(pwd)/.scala/bin

./mill -i copyTo cli.launcher $dest/scala-cli 

export PATH=$dest:$PATH
echo Adding $dest to classpath
ls $dest

./mill -i scala docs_checker/check.scala -- docs/cookbooks 