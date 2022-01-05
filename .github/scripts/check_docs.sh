#!/usr/bin/env bash
set -e

dest="$(pwd)/out/sclicheck/bin"

./mill copyTo cli.launcher "$dest/scala-cli"

echo "Adding $dest to PATH"
export PATH="$dest:$PATH"
ls "$dest"

if [ $# -eq 0 ]
  then
    toCheck=("website/docs/cookbooks" "website/docs/commands")
  else
    toCheck=("$@")
fi

# adding --resource-dirs is a hack to get file watching for free on .md files
scala-cli sclicheck/sclicheck.scala --resource-dirs docs -- "${toCheck[@]}"
