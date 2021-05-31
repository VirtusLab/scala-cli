#!/usr/bin/env bash
set -e

READLINK="readlink"
if [ "$(uname)" = "Darwin" ]; then
  READLINK="greadlink"
  if ! which "$READLINK" >/dev/null 2>&1; then
    echo "greadlink not found. Install it with"
    echo "  brew install coreutils"
    exit 1
  fi
fi
DIR="$(dirname "$("$READLINK" -f "${BASH_SOURCE[0]}")")"
LAUNCHER="$DIR/out/cli/2.12.13/nativeImage/dest/scala"

exec "$LAUNCHER" "$@"
