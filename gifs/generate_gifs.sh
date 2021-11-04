#!/usr/bin/env bash

set -exo pipefail

# Generate svg files for arguments based on create scripts with scenarios

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
OUT=$SCRIPT_DIR/.scala

mkdir -p $OUT 
rm -f $OUT/failures.txt

tty && TTY_OPS="-it"

columns=70
rows=20
no_record=
no_build=
no_gifs=
no_svgs=

for name in "$@"
do

  case $name in

    "--no-record")
      no_record=true
      ;;

    "--no-gifs")
      no_gifs=true
      ;;

    "--no-svgs")
      no_svgs=true
      ;;

    "--no-build")
      no_build=true
      ;;


    *)
      ;;
  esac

done

if [ -z "$no_build" ]; then
  docker build $SCRIPT_DIR --tag gif-renderer  
  docker build $SCRIPT_DIR/svg_render/ --tag svg_rendrer
fi

echo "Option build $no_build gifs $no_gifs svg $no_svgs record $no_record"

for arg in "$@"
do


  if [[ "$arg" == --* ]]; then 
    echo "Skipping $name" 
  else
    fileName=$(basename "$arg")
    name=${fileName%%.sh} 

    echo processing $name with $TTY_OPS
    svg_render_mappings="-v $SCRIPT_DIR/../website/static/img:/data -v $OUT/.scala:/out"
    svg_render_ops="--in /out/$name.cast --width $columns --height $rows --term iterm2 --padding 20"

    # Run the scenario
    failure=

    if [ -z "$no_record" ]; then  
      docker run --rm $TTY_OPS -v  $OUT/.scala:/data/out gif-renderer ./run_scenario.sh $name || (
        echo "Scenario failed: $name" &&
        echo $name >> $OUT/failures.txt &&
        failure=true
      )
    fi

    # do not render gifs without TTY
    if [ -n "$TTY_OPS" ] && [ -z "$failure" ]; then
      if [ -z "$no_svgs" ]; then
        docker run --rm $svg_render_mappings svg_rendrer a $svg_render_ops --out /data/$name.svg --profile "/profiles/light" &&
        docker run --rm $svg_render_mappings svg_rendrer a $svg_render_ops --out /data/dark/$name.svg --profile "/profiles/dark" || (
          echo "Scenario failed: $name" &&
          echo $name >> $OUT/failures.txt &&
          failure=true
        )
      fi
      if [ -z "$no_gifs" ]; then
        docker run --rm $svg_render_mappings asciinema/asciicast2gif -w $columns -h $rows -t monokai /out/$name.cast /data/gifs/$name.gif || (
          echo "Scenario failed: $name" &&
          echo $name >> $OUT/failures.txt &&
          failure=true
        )
      fi
    fi
    echo "done" 
  fi
done

failures=
test -f "$OUT/failures.txt" && failures=$(cat "$OUT/failures.txt") 

if [ -n "$failures" ]; then
  echo "Scenarios failed:" &&
  echo "$failures" &&
  exit 1
else
  echo "All scenarios succeded!"
fi