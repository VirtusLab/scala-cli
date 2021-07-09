#!/usr/bin/env bash
set -euo pipefail
# Build scala-cli native image
LAUNCHER="$(./mill -i show cli.nativeImage | tail -1 |  jq -r |  sed  -E 's/ref:[^:]*://')"
echo $LAUNCHER
# Build pkg package for scala-cli
cs launch org.virtuslab::scala-packager-cli:0.1.12 -- --source-app-path "$LAUNCHER" --output scala-cli.pkg --pkg
# Copy pkg package to artifacts
mkdir -p artifacts
cp ./scala-cli.pkg artifacts/scala-cli.pkg