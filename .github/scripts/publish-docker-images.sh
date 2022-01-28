#!/usr/bin/env bash
set -eu

SCALA_CLI_VERSION="$(./mill -i ci.publishVersion)"

docker tag scala-cli virtuslab/scala-cli:latest
docker tag scala-cli virtuslab/scala-cli:"$SCALA_CLI_VERSION"
docker push virtuslab/scala-cli:latest
docker push virtuslab/scala-cli:"$SCALA_CLI_VERSION"
