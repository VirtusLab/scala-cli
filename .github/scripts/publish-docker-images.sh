#!/usr/bin/env bash
set -eu

launcherMillCommand="ci.publishVersion"
mill="./mill"

SCALA_CLI_VERSION=$("$mill" -i "$launcherMillCommand")

docker tag scala-cli virtuslab/scala-cli:latest
docker tag scala-cli virtuslab/scala-cli:$SCALA_CLI_VERSION
docker tag scala-cli-slim virtuslab/scala-cli-slim:latest
docker tag scala-cli-slim virtuslab/scala-cli-slim:$SCALA_CLI_VERSION
docker push virtuslab/scala-cli:latest
docker push virtuslab/scala-cli:$SCALA_CLI_VERSION
docker push virtuslab/scala-cli-slim:latest
docker push virtuslab/scala-cli-slim:$SCALA_CLI_VERSION
