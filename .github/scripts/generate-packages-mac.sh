#!/usr/bin/env bash
set -euo pipefail
# Build scala-cli native image
LAUNCHER="$(./mill -i show cli-core.nativeImage | jq -r | grep -oE "[^:]+$")"
# Build pkg package for scala-cli
cs launch org.virtuslab::scala-packager-cli:0.1.7 -- --source-app-path "$LAUNCHER" --output scala-cli.pkg --pkg
# Copy pkg package to artifacts
mkdir -p artifacts
cp ./scala-cli.pkg artifacts/scala-cli.pkg