#!/usr/bin/env bash

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
OUT=$SCRIPT_DIR/.scala
mkdir $OUT

docker build gifs --tag gif-renderer  &&
    docker build gifs/svg_render/ --tag svg_rendrer &&
    # generate termnail session
    docker run --rm -ti -v $OUT/.scala:/out gif-renderer generate_gif.sh  $1 &&
    # render svgs
    docker run --rm -v $SCRIPT_DIR/../website/static/img:/data -v $OUT/.scala:/out svg_rendrer a --in /out/$1.cast --out /data/$1.svg --width 70 --height 20 --profile "/profiles/light" --term iterm2 &&
    docker run --rm -v $SCRIPT_DIR/../website/static/img:/data -v $OUT/.scala:/out svg_rendrer a --in /out/$1.cast --out /data/dark/$1.svg --width 70 --height 20 --profile "/profiles/dark" --term iterm2
