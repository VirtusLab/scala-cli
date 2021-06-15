#!/usr/bin/env sh
# Build scala-cli native image
./mill -i show cli-core.nativeImage
# Build deb package for scala-cli
cs launch org.virtuslab::scala-packager-cli:0.1.3 -- --source-app-path ./out/cli-core/nativeImage/dest/scala --output scala-cli.deb --debian
# Copy deb package to artifacts
mkdir artifacts && cp ./scala-cli.deb artifacts/scala-cli.deb