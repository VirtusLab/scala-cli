#!/usr/bin/env bash
set -eu

SCALA_CLI_VERSION="$(./mill -i ci.publishVersion)"

docker tag scala-cli-slim virtuslab/scala-cli-slim:latest
docker tag scala-cli-slim virtuslab/scala-cli-slim:"$SCALA_CLI_VERSION"
docker push virtuslab/scala-cli-slim:latest
docker push virtuslab/scala-cli-slim:"$SCALA_CLI_VERSION"
