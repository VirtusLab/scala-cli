#!/usr/bin/env bash

# Generate svg files for arguments based on create scripts with scenarios

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
OUT=$SCRIPT_DIR/.scala
mkdir $OUT

for name in "$@"
do
  echo processing $name
  svg_render_mappings="-v $SCRIPT_DIR/../website/static/img:/data -v $OUT/.scala:/out"
  svg_render_ops="--in /out/$name.cast --width 70 --height 20 --term iterm2 --padding 20"
  echo "start" &&
    docker build gifs --tag gif-renderer  &&
    docker build gifs/svg_render/ --tag svg_rendrer &&
    # generate termnail session
    docker run --rm -it -v  $OUT/.scala:/out gif-renderer generate_gif.sh  $name &&
    # render svgs
    docker run --rm $svg_render_mappings svg_rendrer a $svg_render_ops --out /data/$name.svg --profile "/profiles/light" &&
    docker run --rm $svg_render_mappings svg_rendrer a $svg_render_ops --out /data/dark/$name.svg --profile "/profiles/dark" &&
    echo "done" 
done