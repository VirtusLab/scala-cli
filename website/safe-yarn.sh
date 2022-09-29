#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

docker image inspect node:16 > /dev/null || docker pull node:16

if [ "$1" == "start" ]; then
  args="$1 -h 0.0.0.0 ${@:2}"
else 
  args="$@"
fi

docker run -p 127.0.0.1:3000:3000 -it -v $SCRIPT_DIR:/data -w /data  node:16 yarn $args