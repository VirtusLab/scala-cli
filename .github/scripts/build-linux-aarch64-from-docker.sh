#!/usr/bin/env bash
# Copied from https://github.com/VirtusLab/coursier-m1/blob/6e54dca5e775c9ed57861ae8d0e2b45602c6d053/.github/scripts/build-linux-aarch64-from-docker.sh
set -e

apt-get update -q -y
apt-get install -q -y build-essential libz-dev zlib1g-dev git python3-pip curl zip

export PATH="$(pwd)/utils:$PATH"

eval "$(cs java --env --jvm 11 --jvm-index https://github.com/coursier/jvm-index/raw/master/index.json)"

git config --global --add safe.directory "$(pwd)"

./mill -i show cli.nativeImage
./mill -i copyDefaultLauncher ./artifacts
if "true" == $(./mill -i ci.shouldPublish); then
  .github/scripts/generate-os-packages.sh
fi
