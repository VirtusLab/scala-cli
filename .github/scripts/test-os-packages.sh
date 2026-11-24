#!/usr/bin/env bash
# Sanity-checks the Linux .deb and .rpm packages built by generate-os-packages.sh:
# installs each of them in a clean container and runs Scala CLI from it, sharing the host coursier
# caches.
set -euo pipefail

ARCHITECTURE="$(uname -m)" # Set the default architecture
if [[ $# -eq 1 ]]; then
  # architecture gets overridden by command line param
  ARCHITECTURE=$1
fi

ARTIFACTS_DIR="$(pwd)/artifacts"

# coursier caches of the host (restored by coursier/cache-action on the CI), shared by all the
# containers below, so that they don't each download the JVM, Bloop, the Scala compiler, etc. again
# from Maven Central
HOST_COURSIER_CACHE="${COURSIER_CACHE:-$HOME/.cache/coursier/v1}"
HOST_COURSIER_ARCHIVE_CACHE="${COURSIER_ARCHIVE_CACHE:-$HOME/.cache/coursier/arc}"
mkdir -p "$HOST_COURSIER_CACHE" "$HOST_COURSIER_ARCHIVE_CACHE"
CACHE_OWNER="$(id -u):$(id -g)"
CONTAINER_IMAGES=()

# the containers run as root, so give the files they wrote back to the host user, so that
# coursier/cache-action can save them (and the host can clean them up)
restore_cache_ownership() {
  if [[ ${#CONTAINER_IMAGES[@]} -gt 0 ]]; then
    docker run --rm \
      -v "$HOST_COURSIER_CACHE:/coursier/v1" \
      -v "$HOST_COURSIER_ARCHIVE_CACHE:/coursier/arc" \
      "${CONTAINER_IMAGES[0]}" \
      chown -R "$CACHE_OWNER" /coursier/v1 /coursier/arc ||
      echo "Warning: could not restore the ownership of the coursier caches" 1>&2
  fi
}
trap restore_cache_ownership EXIT

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
  CONTAINER_IMAGES+=("$image")
  # pass CI through, so that the coursier User-Agent of Scala CLI marks these downloads as CI ones
  docker run --rm -e CI \
    -v "$ARTIFACTS_DIR:/artifacts:ro" \
    -v "$HOST_COURSIER_CACHE:/coursier/v1" \
    -v "$HOST_COURSIER_ARCHIVE_CACHE:/coursier/arc" \
    -e COURSIER_CACHE=/coursier/v1 \
    -e COURSIER_ARCHIVE_CACHE=/coursier/arc \
    "$image" \
    bash -euo pipefail -c "$installCommand /artifacts/$package
$SMOKE_TEST"
}

test_package "debian:stable-slim" "apt-get update -q && DEBIAN_FRONTEND=noninteractive apt-get install -y -q" "scala-cli-$ARCHITECTURE-pc-linux.deb"
test_package "fedora:latest" "dnf install -y -q" "scala-cli-$ARCHITECTURE-pc-linux.rpm"
