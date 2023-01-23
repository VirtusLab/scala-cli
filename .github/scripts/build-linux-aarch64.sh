#!/usr/bin/env bash
# Copied from https://github.com/VirtusLab/coursier-m1/blob/6e54dca5e775c9ed57861ae8d0e2b45602c6d053/.github/scripts/build-linux-aarch64.sh
set -euv

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

mkdir -p artifacts
mkdir -p utils
cp "$(cs get https://github.com/coursier/coursier/releases/download/v2.0.16/cs-aarch64-pc-linux)" utils/cs
chmod +x utils/cs

cp "$DIR/build-linux-aarch64-from-docker.sh" utils/

docker run $(if test -t 1; then echo "-it"; fi) --rm \
  --volume "$(pwd):/data" \
  -w /data \
  -e "CI=$CI" \
  -e "JAVA_OPTS=-Djdk.lang.Process.launchMechanism=vfork" \
  ubuntu:20.04 \
    /data/utils/build-linux-aarch64-from-docker.sh
