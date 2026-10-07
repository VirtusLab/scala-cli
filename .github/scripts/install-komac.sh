#!/usr/bin/env bash
set -euo pipefail

# Installs komac (https://github.com/russellbanks/Komac) on a Windows runner
# and adds it to the PATH of subsequent GitHub Actions steps.

KOMAC_VERSION="${KOMAC_VERSION:-2.16.0}"

komac_dir="$(cygpath -u "$RUNNER_TEMP")/komac"
mkdir -p "$komac_dir"
curl -fsSL -o "$komac_dir/komac.exe" \
  "https://github.com/russellbanks/Komac/releases/download/v$KOMAC_VERSION/komac-$KOMAC_VERSION-x86_64-pc-windows-msvc.exe"
cygpath -w "$komac_dir" >>"$GITHUB_PATH"
