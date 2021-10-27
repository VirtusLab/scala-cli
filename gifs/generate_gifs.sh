#!/usr/bin/env bash

set -e

# Generate svg files for arguments based on create scripts with scenarios

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
OUT=$SCRIPT_DIR/.scala

test -d $OUT || mkdir $OUT 
test -f $OUT/failures.txt && rm $OUT/failures.txt

docker build gifs --tag gif-renderer  
docker build gifs/svg_render/ --tag svg_rendrer

for name in "$@"
do
  echo processing $name
  svg_render_mappings="-v $SCRIPT_DIR/../website/static/img:/data -v $OUT/.scala:/out"
  svg_render_ops="--in /out/$name.cast --width 70 --height 20 --term iterm2 --padding 20"
  echo "start" 

  # generate termnail session
  docker run --rm -it -v  $OUT/.scala:/data/out gif-renderer ./run_scenario.sh  $name || (
    echo "Scenario failed: $name" &&
    echo $name >> $OUT/failures.txt
  )
  # render svgs
  if [ -z "$SKIP_RENDER" ]; then
    docker run --rm $svg_render_mappings svg_rendrer a $svg_render_ops --out /data/$name.svg --profile "/profiles/light"
    docker run --rm $svg_render_mappings svg_rendrer a $svg_render_ops --out /data/dark/$name.svg --profile "/profiles/dark"
  fi

  echo "done" 
done

test -f $OUT/failures.txt && (
  echo "Scenarios failed:" &&
  cat $OUT/failures.txt &&
  exit 1
) || echo "All scenarios succeded!"