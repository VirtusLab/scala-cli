#!/usr/bin/env bash
set -e

COMMAND="cli-cross[3.1.1].base-image.writeNativeImageScript"

# Using 'mill -i' so that the Mill process doesn't outlive this invocation

if [[ "$OSTYPE" == "msys" ]]; then
  ./mill.bat -i ci.copyJvm --dest jvm
  export JAVA_HOME="$(pwd -W | sed 's,/,\\,g')\\jvm"
  export GRAALVM_HOME="$JAVA_HOME"
  export PATH="$(pwd)/bin:$PATH"
  echo "PATH=$PATH"
  ./mill.bat -i "$COMMAND" generate-native-image.bat ""
  ./generate-native-image.bat
else
  if [ $# == "0" ]; then
    if [[ "$OSTYPE" == "linux-gnu" ]]; then
      COMMAND="cli-cross[3.1.1].linux-docker-image.writeNativeImageScript"
      CLEANUP=("sudo" "rm" "-rf" "out/cli/linux-docker-image/nativeImageDockerWorkingDir")
    else
      CLEANUP=("true")
    fi
  else
    case "$1" in
      "static")
        COMMAND="cli-cross[3.1.1].static-image.writeNativeImageScript"
        CLEANUP=("sudo" "rm" "-rf" "out/cli/static-image/nativeImageDockerWorkingDir")
        ;;
      "mostly-static")
        COMMAND="cli-cross[3.1.1].mostly-static-image.writeNativeImageScript"
        CLEANUP=("sudo" "rm" "-rf" "out/cli/mostly-static-image/nativeImageDockerWorkingDir")
        ;;
      *)
        echo "Invalid image name: $1" 1>&2
        exit 1
        ;;
    esac
  fi

  ./mill -i "$COMMAND" generate-native-image.sh ""
  bash ./generate-native-image.sh
  "${CLEANUP[@]}"
fi
