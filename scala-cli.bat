@echo off

rem This is the launcher script of Scala CLI (https://github.com/VirtusLab/scala-cli).
rem This script downloads and runs the Scala CLI version set by SCALA_CLI_VERSION below.
rem
rem Download the latest version of this script at https://github.com/VirtusLab/scala-cli/raw/main/scala-cli.bat

setlocal enabledelayedexpansion

set "SCALA_CLI_VERSION=0.1.12"

set SCALA_CLI_URL=https://github.com/VirtusLab/scala-cli/releases/download/v%SCALA_CLI_VERSION%/scala-cli.bat
set CACHE_BASE=%localappdata%/Coursier/v1

set CACHE_DEST=%CACHE_BASE%/https/github.com/VirtusLab/scala-cli/releases/download/v%SCALA_CLI_VERSION%
set SCALA_CLI_BIN_PATH=%CACHE_DEST%/scala-cli.bat

if not exist "%SCALA_CLI_BIN_PATH%" (

    if not exist "%CACHE_DEST%" mkdir "%CACHE_DEST%"
     
    where /Q curl
    if %ERRORLEVEL% EQU 0 (
        curl -fLo "%SCALA_CLI_BIN_PATH%" "%SCALA_CLI_URL%"
    ) else (
        echo Could not download scala-cli %SCALA_CLI_VERSION%. Please, install curl and run './scala-cli.bat' again.
        exit 1
    )
)

%SCALA_CLI_BIN_PATH% %*
