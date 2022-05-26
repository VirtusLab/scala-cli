#!/usr/bin/env bash
set -e

yarn --cwd website install
yarn --cwd website build
