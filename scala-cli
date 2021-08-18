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
LAUNCHER_DIR="$DIR/out/cli/nativeImage/dest"

if [ ! -x "$LAUNCHER_DIR/scala" ]; then
  echo "native-image launcher not built yet." 1>&2
  echo "In order to build it, go in $DIR, and run" 1>&2
  echo 1>&2
  echo "  ./mill cli.nativeImage" 1>&2
  exit 1
fi

export PATH="$LAUNCHER_DIR:$PATH"
exec scala "$@"
