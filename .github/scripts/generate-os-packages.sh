#!/usr/bin/env bash
set -eu

ARCHITECTURE="x86_64" # Set the default architecture
if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "darwin"* ]]; then
  # When running on macOS and Linux lets determine the architecture
  ARCHITECTURE=$(uname -m)
fi
if [[ $# -eq 1 ]]; then
  # architecture gets overridden by command line param
  ARCHITECTURE=$1
fi

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
    --maintainer "scala-cli@virtuslab.com" \
    --launcher-app "scala-cli" \
    --priority "optional" \
    --section "devel"
  mv "$ARTIFACTS_DIR/scala-cli.deb" "$ARTIFACTS_DIR/scala-cli-x86_64-pc-linux.deb"
}

generate_rpm() {
  packager \
    --rpm \
    --version "$(shortVersion)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli-x86_64-pc-linux.rpm" \
    --description "Scala CLI" \
    --maintainer "scala-cli@virtuslab.com" \
    --license "ASL 2.0" \
    --launcher-app "scala-cli"
}

generate_pkg() {
  arch=$1
  packager \
    --pkg \
    --version "$(version)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli-$arch-apple-darwin.pkg" \
    --identifier "scala-cli" \
    --launcher-app "scala-cli"
}

generate_msi() {

  # Having the MSI automatically install Visual C++ redistributable when needed,
  # see https://wixtoolset.org/documentation/manual/v3/howtos/redistributables_and_install_checks/install_vcredist.html
  "$mill" -i ci.writeWixConfigExtra wix-visual-cpp-redist.xml

  packager \
    --msi \
    --version "$(shortVersion)" \
    --source-app-path "$(launcher)" \
    --output "$ARTIFACTS_DIR/scala-cli-x86_64-pc-win32.msi" \
    --product-name "Scala CLI" \
    --maintainer "scala-cli@virtuslab.com" \
    --launcher-app "scala-cli" \
    --license-path "./LICENSE" \
    --exit-dialog "To run Scala CLI, open a Command window, and type scala-cli + Enter. If scala-cli cannot be found, ensure that the Command window was opened after Scala CLI was installed." \
    --logo-path "./logo.png" \
    --suppress-validation \
    --extra-configs wix-visual-cpp-redist.xml \
    --wix-upgrade-code-guid "C74FC9A1-9381-40A6-882F-9044C603ABD9"
  rm -f "$ARTIFACTS_DIR/"*.wixpdb || true
}

generate_sdk() {
  local sdkDirectory
  local binName

  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    if [[ "$ARCHITECTURE" == "aarch64" ]] || [[ "$ARCHITECTURE" == "x86_64" ]]; then
      sdkDirectory="scala-cli-$ARCHITECTURE-pc-linux-static-sdk"
    else
      echo "scala-cli is not supported on $ARCHITECTURE"
      exit 2
    fi
    binName="scala-cli"
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    if [[ "$ARCHITECTURE" == "arm64" ]]; then
      sdkDirectory="scala-cli-aarch64-apple-darwin-sdk"
    else
      sdkDirectory="scala-cli-x86_64-apple-darwin-sdk"
    fi
    binName="scala-cli"
  elif [[ "$OSTYPE" == "msys" ]]; then
    sdkDirectory="scala-cli-x86_64-pc-win32-sdk"
    binName="scala-cli.exe"
  else
    echo "Unrecognized operating system: $OSTYPE" 1>&2
    exit 1
  fi

  mkdir -p "$sdkDirectory"/bin
  cp "$(launcher)" "$sdkDirectory"/bin/"$binName"

  if [[ "$OSTYPE" == "msys" ]]; then
    7z a "$sdkDirectory".zip "$sdkDirectory"
  else
    zip -r "$sdkDirectory".zip "$sdkDirectory"
  fi

  mv "$sdkDirectory".zip "$ARTIFACTS_DIR/"/"$sdkDirectory".zip
}

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  if [ "$ARCHITECTURE" == "x86_64" ]; then
    generate_deb
    generate_rpm
  fi
  generate_sdk
elif [[ "$OSTYPE" == "darwin"* ]]; then
  if [ "$ARCHITECTURE" == "arm64" ]; then
    generate_pkg "aarch64"
  else
    generate_pkg "x86_64"
  fi
  generate_sdk
elif [[ "$OSTYPE" == "msys" ]]; then
  generate_msi
  generate_sdk
else
  echo "Unrecognized operating system: $OSTYPE" 1>&2
  exit 1
fi
