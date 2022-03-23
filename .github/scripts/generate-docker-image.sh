#!/usr/bin/env bash
set -eu

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WORKDIR="$ROOT/out/docker-workdir"

mkdir -p "$WORKDIR"
./mill -i copyTo cli-cross[3.1.1].nativeImageStatic "$WORKDIR/scala-cli" 1>&2

cd "$WORKDIR"
docker build -t scala-cli -f "$ROOT/.github/scripts/docker/ScalaCliDockerFile" .
