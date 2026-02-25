#!/usr/bin/env bash
set -e

# Verifies that the native launcher runs on older x86_64 CPUs (without AVX/AVX2/FMA).
# Uses QEMU user-mode emulation with a Westmere CPU model (2010, SSE4.2 but no AVX).

LAUNCHER_GZ="${1:?Usage: $0 <launcher.gz>}"

sudo apt-get update -qq && sudo apt-get install -y -qq qemu-user > /dev/null

LAUNCHER="/tmp/scala-cli-compat-test"
gunzip -c "$LAUNCHER_GZ" > "$LAUNCHER"
chmod +x "$LAUNCHER"

echo "Running native launcher under QEMU with Westmere CPU (no AVX/AVX2/FMA)..."
qemu-x86_64 -cpu Westmere "$LAUNCHER" version
echo "CPU compatibility check passed."
