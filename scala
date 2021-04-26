#!/usr/bin/env bash
set -e

DIR="$(dirname "${BASH_SOURCE[0]}")"
LAUNCHER="$DIR/out/cli/2.12.13/nativeImage/dest/scala"

exec "$LAUNCHER" "$@"
