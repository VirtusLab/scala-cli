#!/usr/bin/env bash
set -euo pipefail

# Submits a new Scala CLI version to WinGet (https://github.com/microsoft/winget-pkgs) with komac.
# Requires GITHUB_TOKEN (with the public_repo scope) and KOMAC_FORK_OWNER to be set.
# The version defaults to the current tag, without the "v" prefix.

version="${1:-${GITHUB_REF_NAME#v}}"

# komac infers the architecture from the URL, and "win32" in the MSI name makes it pick x86,
# which the WinGet validation rejects, as the MSI is x64; "|x64" overrides that
url="https://github.com/VirtusLab/scala-cli/releases/download/v$version/scala-cli-x86_64-pc-win32.msi|x64"

komac sync-fork
komac update VirtusLab.ScalaCLI --version "$version" --urls "$url" --submit
