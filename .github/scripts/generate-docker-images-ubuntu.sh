#!/usr/bin/env bash
set -eu

launcherMillCommand="cli.nativeImageMostlyStatic"
launcherName="scala-cli"
mill="./mill"

"$mill" -i copyTo "$launcherMillCommand" "$launcherName" 1>&2
echo "$launcherName"

docker build -t scala-cli -f ./.github/scripts/docker/ScalaCliDockerFile .
docker build -t scala-cli-slim -f ./.github/scripts/docker/ScalaCliSlimDockerFile .
