#!/usr/bin/env bash
set -eu

# This script automatically download scala-cli from latest release and cache via coursier

# https://gist.github.com/lukechilds/a83e1d7127b78fef38c2914c4ececc3c
LATATEST_TAG=$(
    curl --silent "https://api.github.com/repos/VirtusLab/scala-cli/releases/latest" | grep '"tag_name":' | sed 's/.*"\(v.*\)",/\1/'
)

if [ "$(expr substr $(uname -s) 1 5 2>/dev/null)" == "Linux" ]; then
  SCALA_CLI_URL="https://github.com/VirtusLab/scala-cli/releases/download/$LATATEST_TAG/scala-cli-x86_64-pc-linux.gz"
  CACHE_BASE="$HOME/.cache/coursier/v1"
else
  SCALA_CLI_URL="https://github.com/VirtusLab/scala-cli/releases/download/$LATATEST_TAG/scala-cli-x86_64-apple-darwin.gz"
  CACHE_BASE="$HOME/Library/Caches/Coursier/v1"
fi

CACHE_DEST="$CACHE_BASE/$(echo "$SCALA_CLI_URL" | sed 's@://@/@')"
SCALA_CLI_BIN_PATH=${CACHE_DEST%.gz}

if [ ! -f "$CACHE_DEST" ]; then
  mkdir -p "$(dirname "$CACHE_DEST")"
  TMP_DEST="$CACHE_DEST.tmp-setup"
  echo "Downloading $SCALA_CLI_URL"
  curl -fLo "$TMP_DEST" "$SCALA_CLI_URL"
  mv "$TMP_DEST" "$CACHE_DEST"
  gunzip -k "$CACHE_DEST"
  chmod +x "$SCALA_CLI_BIN_PATH"
fi

exec "$SCALA_CLI_BIN_PATH" "$@"