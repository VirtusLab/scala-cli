#!/usr/bin/env bash
set -eu

ARTIFACTS_DIR="artifacts/"
mkdir -p "$ARTIFACTS_DIR"

if [[ "$OSTYPE" == "msys" ]]; then
  mill="./mill.bat"
else
  mill="./mill"
fi

packager() {
  "$mill" -i packager.run "$@"
}

launcher() {
  local launcherMillCommand
  local launcherName
  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # requires less memory - to be used when memory is tight on the CI
    launcherMillCommand="cli-core.nativeImage"
    launcherName="scala"
  elif [[ "$OSTYPE" == "msys" ]]; then
    # not enough memory on the GitHub actions Windows workers to build a native image
    launcherMillCommand="cli.standaloneLauncher"
    launcherName="scala.bat"
  else
    launcherMillCommand="cli.nativeImage"
    launcherName="scala"
  fi

  "$mill" -i copyTo "$launcherMillCommand" "$launcherName" 1>&2
  echo "$launcherName"
}

version() {
  "$mill" -i writePackageVersionTo scala-cli-version 1>&2
  cat scala-cli-version
}

shortVersion() {
  "$mill" -i writeShortPackageVersionTo scala-cli-short-version 1>&2
  cat scala-cli-short-version
}

generate_deb() {
  packager \
    --deb \
    --version "$(version)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli.deb" \
    --description "Scala CLI"
}

generate_rpm() {
  packager \
    --rpm \
    --version "$(shortVersion)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli.rpm" \
    --description "Scala CLI" \
    --maintainer "Scala CLI"
}

generate_pkg() {
  packager \
    --pkg \
    --version "$(version)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli.pkg"
}

generate_msi() {
  packager \
    --msi \
    --version "$(shortVersion)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli.msi" \
    --product-name "Scala CLI" \
    --maintainer "Scala CLI"
}

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  generate_deb
  generate_rpm
elif [[ "$OSTYPE" == "darwin"* ]]; then
  generate_pkg
elif [[ "$OSTYPE" == "msys" ]]; then
  generate_msi
else
  echo "Unrecognized operating system: $OSTYPE" 1&2
  exit 1
fi
