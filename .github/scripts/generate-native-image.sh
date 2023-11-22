#!/usr/bin/env bash
set -e

COMMAND="cli.base-image.writeNativeImageScript"

# temporary, until we pass JPMS options to native-image,
# see https://www.graalvm.org/release-notes/22_2/#native-image
export USE_NATIVE_IMAGE_JAVA_PLATFORM_MODULE_SYSTEM=false

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
      COMMAND="cli.linux-docker-image.writeNativeImageScript"
      CLEANUP=("sudo" "rm" "-rf" "out/cli/linux-docker-image/nativeImageDockerWorkingDir")
    else
      CLEANUP=("true")
    fi
  else
    case "$1" in
      "static")
        COMMAND="cli.static-image.writeNativeImageScript"
        CLEANUP=("sudo" "rm" "-rf" "out/cli/static-image/nativeImageDockerWorkingDir")
        ;;
      "mostly-static")
        COMMAND="cli.mostly-static-image.writeNativeImageScript"
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
