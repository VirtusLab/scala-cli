#!/usr/bin/env bash
set -euo pipefail
# Build scala-cli native image
LAUNCHER="$(./mill.bat -i show cli.standaloneLauncher | jq -r |  sed  -E 's/ref:[^:]*://')"
echo $LAUNCHER
# Build msi package for scala-cli
cs launch org.virtuslab::scala-packager-cli:0.1.12 -- --source-app-path "$LAUNCHER" --output scala-cli.msi --msi --product-name 'Scala CLI' --maintainer 'Scala CLI'
# Copy msi package to artifacts
mkdir -p artifacts
ls artifacts
mv ./scala-cli.msi artifacts/scala-cli.msi
echo "artifacts/"
ls artifacts