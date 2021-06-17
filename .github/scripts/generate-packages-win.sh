#!/usr/bin/env bash
set -euo pipefail
# Build scala-cli native image
LAUNCHER="$(./mill -i show cli-core.standaloneLauncher | jq -r | grep -oE "[^:]+$")"
# Build msi package for scala-cli
cs launch org.virtuslab::scala-packager-cli:0.1.7 -- --source-app-path "$LAUNCHER" --output scala-cli.msi --msi
# Copy msi package to artifacts
mkdir -p artifacts
cp ./scala-cli.msi artifacts/scala-cli.msi