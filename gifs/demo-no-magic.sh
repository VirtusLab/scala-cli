# Mock of demo magic, for running on CI

function p() {
  echo "running: $@"
}

function pe() {
  p "$@"
  run_cmd "$@"
}

function pei {
  NO_WAIT=true pe "$@"
}

function cmd() {
  run_cmd "${command}"
}

function run_cmd() {
  eval "$@"
}

function updateFile(){
  rm -f $1
  if [ $# -eq 1 ]; then
    while IFS= read -r data; do echo "$data" >> $1 ; done;
  else
    echo $2 > $1
  fi

  p "cat $1"
  rougify --theme tulip $1
  
  doSleep 1
}

function clearConsole(){
  echo clear 
}

function doSleep(){
  echo sleep $1
}