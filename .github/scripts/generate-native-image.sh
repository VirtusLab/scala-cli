#!/usr/bin/env bash
set -e

COMMAND="cli[].base-image.writeDefaultNativeImageScript"

# temporary, until we pass JPMS options to native-image,
# see https://www.graalvm.org/release-notes/22_2/#native-image
export USE_NATIVE_IMAGE_JAVA_PLATFORM_MODULE_SYSTEM=false

export MSYS_NO_PATHCONV=1 # prevent /d from being converted to d:\
export MSYS2_ARG_CONV_EXCL="*"

function setCodePage {
  local CODEPAGE=$1 ; shift
  reg add "HKLM\SYSTEM\CurrentControlSet\Control\Nls\CodePage" /v ACP /t REG_SZ /d $CODEPAGE /f
}
function getCodePage {
  reg query "HKLM\SYSTEM\CurrentControlSet\Control\Nls\CodePage" /v ACP | grep '[0-9]' | sed -E -e 's#[^0-9]*$##' -e 's#^.*[^0-9]##'
}
SAVED_CODEPAGE=`getCodePage`
echo "SAVED_CODEPAGE[$SAVED_CODEPAGE]" 1>&2

function atexit {
  if [ -n "$SAVED_CODEPAGE" ]; then
    set -x
    reg add "HKLM\SYSTEM\CurrentControlSet\Control\Nls\CodePage" /v ACP /t REG_SZ /d $SAVED_CODEPAGE /f
  fi
}

# Using 'mill -i' so that the Mill process doesn't outlive this invocation
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
  trap atexit EXIT INT TERM
  setCodePage 65001 # set code page to UTF-8 before GraalVM compile

  ./mill.bat -i ci.copyJvm --dest jvm
  export JAVA_HOME="$(pwd -W | sed 's,/,\\,g')\\jvm"
  export GRAALVM_HOME="$JAVA_HOME"
  export PATH="$(pwd)/bin:$PATH"
  echo "PATH=$PATH"

  # this part runs into connection problems on Windows, so we retry up to 5 times
  MAX_RETRIES=5
  RETRY_COUNT=0
  while (( RETRY_COUNT < MAX_RETRIES )); do
      ./mill.bat -i "$COMMAND" --scriptDest generate-native-image.bat

      if [[ $? -ne 0 ]]; then
          echo "Error occurred during 'mill.bat -i $COMMAND generate-native-image.bat' command. Retrying... ($((RETRY_COUNT + 1))/$MAX_RETRIES)"
          (( RETRY_COUNT++ ))
          sleep 2
      else
          ./generate-native-image.bat
          if [[ $? -ne 0 ]]; then
              echo "Error occurred during 'generate-native-image.bat'. Retrying... ($((RETRY_COUNT + 1))/$MAX_RETRIES)"
              (( RETRY_COUNT++ ))
              sleep 2
          else
              echo "'generate-native-image.bat' succeeded with $RETRY_COUNT retries."
              break
          fi
      fi
  done

  if (( RETRY_COUNT == MAX_RETRIES )); then
      echo "Exceeded maximum retry attempts. Exiting with error."
      exit 1
  fi
else
  if [ $# == "0" ]; then
    if [[ "$OSTYPE" == "linux-gnu" ]]; then
      COMMAND="cli[].linux-docker-image.writeDefaultNativeImageScript"
      CLEANUP=("sudo" "rm" "-rf" "out/cli/linux-docker-image/nativeImageDockerWorkingDir")
    else
      CLEANUP=("true")
    fi
  else
    case "$1" in
      "static")
        COMMAND="cli[].static-image.writeDefaultNativeImageScript"
        CLEANUP=("sudo" "rm" "-rf" "out/cli/static-image/nativeImageDockerWorkingDir")
        ;;
      "mostly-static")
        COMMAND="cli[].mostly-static-image.writeDefaultNativeImageScript"
        CLEANUP=("sudo" "rm" "-rf" "out/cli/mostly-static-image/nativeImageDockerWorkingDir")
        ;;
      *)
        echo "Invalid image name: $1" 1>&2
        exit 1
        ;;
    esac
  fi

  ./mill -i "$COMMAND" --scriptDest generate-native-image.sh
  bash ./generate-native-image.sh
  "${CLEANUP[@]}"
fi
