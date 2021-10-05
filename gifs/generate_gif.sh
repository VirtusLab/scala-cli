#!/usr/bin/env bash

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
OUT=$SCRIPT_DIR/.scala
mkdir $OUT

docker build gifs --tag gif-renderer  && docker run --rm -ti -v $OUT/.scala:/out gif-renderer generate_gif.sh  $1 &&
    docker run --rm -v $SCRIPT_DIR/../website/static/img:/data -v $OUT/.scala:/out asciinema/asciicast2gif -w 80 -h 24 -t asciinema /out/$1.cast /data/$1.gif &&
    docker run --rm -v $SCRIPT_DIR/../website/static/img:/data -v $OUT/.scala:/out asciinema/asciicast2gif -w 80 -h 24 -t monokai /out/$1.cast /data/dark/$1.gif
