#!/usr/bin/env bash
set -eu

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WORKDIR="$ROOT/out/docker-slim-workdir"

mkdir -p "$WORKDIR"
./mill -i copyTo cli.nativeImageMostlyStatic "$WORKDIR/scala-cli" 1>&2

cd "$WORKDIR"
docker build -t scala-cli-slim -f "$ROOT/.github/scripts/docker/ScalaCliSlimDockerFile" .
