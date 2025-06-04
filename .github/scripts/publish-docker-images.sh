#!/usr/bin/env bash
set -eu

RAW_VERSION="$(./mill -i ci.publishVersion)"
SCALA_CLI_VERSION="${RAW_VERSION##* }"

docker tag scala-cli virtuslab/scala-cli:latest
docker tag scala-cli virtuslab/scala-cli:"$SCALA_CLI_VERSION"
docker push virtuslab/scala-cli:latest
docker push virtuslab/scala-cli:"$SCALA_CLI_VERSION"
