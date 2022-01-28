#!/usr/bin/env bash
set -e

COMMAND="cli.base-image.writeNativeImageScript"

# Using 'mill -i' so that the Mill process doesn't outlive this invocation

if [[ "$OSTYPE" == "msys" ]]; then
  ./mill.bat -i ci.copyJvm --dest jvm
  export JAVA_HOME="$(pwd -W | sed 's,/,\\,g')\\jvm"
  export GRAALVM_HOME="$JAVA_HOME"
  export PATH="$(pwd)/bin:$PATH"
  echo "PATH=$PATH"
  ./mill.bat -i "$COMMAND" generate-native-image.bat
  # Ideally, the generated script should create that directory itself
  mkdir -p out/cli/base-image/nativeImage/dest
  ./generate-native-image.bat
  # Ideally, the generated script should write the generated launcher there
  cp out/cli/base-image/nativeImageScript/dest/scala-cli.exe out/cli/base-image/nativeImage/dest/scala-cli.exe
else
  if [ $# == "0" ]; then
    if [[ "$OSTYPE" == "linux-gnu" ]]; then
      COMMAND="cli.linux-docker-image.writeNativeImageScript"
      CLEANUP=("sudo" "rm" "-rf" "out/cli/linux-docker-image/nativeImageDockerWorkingDir")
      # Ideally, the generated script should create that directory itself
      mkdir -p out/cli/linux-docker-image/nativeImage/dest/
    else
      CLEANUP=("true")
      # Ideally, the generated script should create that directory itself
      mkdir -p out/cli/base-image/nativeImage/dest
    fi
  else
    case "$1" in
      "static")
        COMMAND="cli.static-image.writeNativeImageScript"
        CLEANUP=("sudo" "rm" "-rf" "out/cli/static-image/nativeImageDockerWorkingDir")
        # Ideally, the generated script should create that directory itself
        mkdir -p out/cli/static-image/nativeImage/dest
        ;;
      "mostly-static")
        COMMAND="cli.mostly-static-image.writeNativeImageScript"
        CLEANUP=("sudo" "rm" "-rf" "out/cli/mostly-static-image/nativeImageDockerWorkingDir")
        # Ideally, the generated script should create that directory itself
        mkdir -p out/cli/mostly-static-image/nativeImage/dest
        ;;
      *)
        echo "Invalid image name: $1" 1>&2
        exit 1
        ;;
    esac
  fi

  ./mill -i "$COMMAND" generate-native-image-0.sh
  # Small mill-native-image issue
  sed 's/nativeImageScript/nativeImage/g' < generate-native-image-0.sh > generate-native-image.sh
  bash ./generate-native-image.sh
  "${CLEANUP[@]}"
fi
