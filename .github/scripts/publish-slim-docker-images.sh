#!/usr/bin/env bash
set -eu

RAW_VERSION="$(./mill -i ci.publishVersion)"
SCALA_CLI_VERSION="${RAW_VERSION##* }"

docker tag scala-cli-slim virtuslab/scala-cli-slim:latest
docker tag scala-cli-slim virtuslab/scala-cli-slim:"$SCALA_CLI_VERSION"
docker push virtuslab/scala-cli-slim:latest
docker push virtuslab/scala-cli-slim:"$SCALA_CLI_VERSION"
