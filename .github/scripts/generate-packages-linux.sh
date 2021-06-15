#!/usr/bin/env bash
set -euo pipefail
# Build scala-cli native image
LAUNCHER="$(./mill -i show cli-core.nativeImage | jq -r | grep -oE "[^:]+$")"
# Build deb package for scala-cli
cs launch org.virtuslab::scala-packager-cli:0.1.3 -- --source-app-path "$LAUNCHER" --output scala-cli.deb --debian
# Copy deb package to artifacts
mkdir -p artifacts
cp ./scala-cli.deb artifacts/scala-cli.deb