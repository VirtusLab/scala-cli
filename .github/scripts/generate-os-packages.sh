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
  local launcherMillCommand="cli.nativeImage"
  local launcherName

  if [[ "$OSTYPE" == "msys" ]]; then
    launcherName="scala.exe"
  else
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
    --description "Scala CLI" \
    --maintainer "Scala CLI" \
    --launcher-app-name "scala-cli"
  mv "$ARTIFACTS_DIR/scala-cli.deb" "$ARTIFACTS_DIR/scala-cli-x86_64-pc-linux.deb"
}

generate_rpm() {
  packager \
    --rpm \
    --version "$(shortVersion)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli-x86_64-pc-linux.rpm" \
    --description "Scala CLI" \
    --maintainer "Scala CLI" \
    --license "ASL 2.0" \
    --launcher-app-name "scala-cli"
}

generate_pkg() {
  packager \
    --pkg \
    --version "$(version)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli-x86_64-apple-darwin.pkg" \
    --identifier "scala-cli" \
    --launcher-app-name "scala-cli"
}

generate_msi() {
  packager \
    --msi \
    --version "$(shortVersion)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli-x86_64-pc-win32.msi" \
    --product-name "Scala CLI" \
    --maintainer "Scala CLI" \
    --launcher-app-name "scala-cli" \
    --license-path "./LICENSE" \
    --exit-dialog "If scala-cli is not visible, please restart your opened consoles" \
    --logo-path "./logo.png" \
    --suppress-validation
  rm -f "$ARTIFACTS_DIR/"*.wixpdb || true
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
