#!/usr/bin/env bash

# from https://github.com/lampepfl/dotty/blob/37e997abc2bf4d42321492acaf7f7832ee7ce146/.github/workflows/scripts/publish-sdkman.sh
# This is script for publishing Scala CLI on SDKMAN.
# It's releasing and announcing the release of Scala CLI on SDKMAN.
#
# Requirement:
#   - the latest stable version of Scala CLI should be available in github artifacts

set -eu

version() {
  "./mill" -i writePackageVersionTo scala-cli-version 1>&2
  cat scala-cli-version
}

SCALA_CLI_VERSION="$(version)"
UNAMES=("pc-linux-static-sdk" "apple-darwin-sdk" "pc-win32-sdk")
PLATFORMS=("LINUX_64" "MAC_OSX" "WINDOWS_64")

for i in "${!PLATFORMS[@]}"; do

    SCALA_CLI_URL="https://github.com/VirtuslabRnD/scala-cli/releases/download/v$SCALA_CLI_VERSION/scala-cli-x86_64-${UNAMES[i]}.zip"

    # Release a new Candidate Version
    curl --silent --show-error --fail \
        -X POST \
        -H "Consumer-Key: $SDKMAN_KEY" \
        -H "Consumer-Token: $SDKMAN_TOKEN" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d '{"candidate": "scalacli", "version": "'"$SCALA_CLI_VERSION"'", "url": "'"$SCALA_CLI_URL"'", "platform": "'"${PLATFORMS[i]}"'" }' \
        https://vendors.sdkman.io/release

    if [[ $? -ne 0 ]]; then
    echo "Fail sending POST request to releasing Scala CLI on SDKMAN on platform: ${PLATFORMS[i]}."
    exit 1
    fi

done

# Set SCALA_CLI_VERSION as Default for Candidate
curl --silent --show-error --fail \
    -X PUT \
    -H "Consumer-Key: $SDKMAN_KEY" \
    -H "Consumer-Token: $SDKMAN_TOKEN" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d '{"candidate": "scalacli", "version": "'"$SCALA_CLI_VERSION"'" }' \
    https://vendors.sdkman.io/default

if [[ $? -ne 0 ]]; then
  echo "Fail sending PUT request to announcing the release of Scala CLI on SDKMAN."
  exit 1
fi