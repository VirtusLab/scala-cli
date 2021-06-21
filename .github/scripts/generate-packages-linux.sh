#!/usr/bin/env bash
set -euo pipefail
# Build scala-cli native image
LAUNCHER="$(./mill -i show cli-core.nativeImage | jq -r | grep -oE "[^:]+$")"
# Build deb package for scala-cli
cs launch org.virtuslab::scala-packager-cli:0.1.10 -- --source-app-path "$LAUNCHER" --output scala-cli.deb --deb
cs launch org.virtuslab::scala-packager-cli:0.1.10 -- --source-app-path "$LAUNCHER" --output scala-cli.rpm --rpm
# Copy deb package to artifacts
mkdir -p artifacts
cp ./scala-cli.deb artifacts/scala-cli.deb
cp ./scala-cli.rpm artifacts/scala-cli.rpm