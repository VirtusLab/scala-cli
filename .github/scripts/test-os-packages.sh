#!/usr/bin/env bash
# Sanity-checks the Linux .deb and .rpm packages built by generate-os-packages.sh:
# installs each of them in a clean container and runs Scala CLI from it.
set -euo pipefail

ARCHITECTURE="$(uname -m)" # Set the default architecture
if [[ $# -eq 1 ]]; then
  # architecture gets overridden by command line param
  ARCHITECTURE=$1
fi

ARTIFACTS_DIR="$(pwd)/artifacts"

SMOKE_TEST='
scala-cli version
output="$(scala-cli -e "println(\"Hello world\")")"
echo "$output"
if [[ "$output" != *"Hello world"* ]]; then
  echo "Unexpected output from the Hello world snippet" 1>&2
  exit 1
fi
'

test_package() {
  local image=$1
  local installCommand=$2
  local package=$3

  echo "Testing $package on $image"
  docker run --rm -v "$ARTIFACTS_DIR:/artifacts:ro" "$image" \
    bash -euo pipefail -c "$installCommand /artifacts/$package
$SMOKE_TEST"
}

test_package "debian:stable-slim" "apt-get update -q && DEBIAN_FRONTEND=noninteractive apt-get install -y -q" "scala-cli-$ARCHITECTURE-pc-linux.deb"
test_package "fedora:latest" "dnf install -y -q" "scala-cli-$ARCHITECTURE-pc-linux.rpm"
