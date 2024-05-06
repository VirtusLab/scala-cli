#!/usr/bin/env bash
set -e

CS_VERSION="2.1.10"

DIR="$(cs get --archive "https://github.com/coursier/coursier/releases/download/v$CS_VERSION/cs-x86_64-pc-win32.zip")"

cp "$DIR/"*.exe cs.exe
