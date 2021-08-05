#!/usr/bin/env bash
set -eu

launcherMillCommand="cli-core.nativeImageMostlyStatic"
launcherName="scala"
mill="./mill"

"$mill" -i copyTo "$launcherMillCommand" "$launcherName" 1>&2
echo "$launcherName"

docker build -t scala-cli -f ./.github/scripts/docker/DockerFile .