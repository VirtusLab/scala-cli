@echo off

rem This is a wrapper script, that automatically download mill via coursier
rem You can give the required mill version with --mill-version parameter
rem If no version is given, it falls back to the value of DEFAULT_MILL_VERSION
rem
rem Project page: https://github.com/coursier/millw
rem Original project page: https://github.com/lefou/millw
rem Script Version: 0.4.0-cs
rem
rem If you want to improve this script, please also contribute your changes back!
rem
rem Licensed under the Apache License, Version 2.0

rem setlocal seems to be unavailable on Windows 95/98/ME
rem but I don't think we need to support them in 2019
setlocal enabledelayedexpansion

set "DEFAULT_MILL_VERSION=0.9.5"

set "MILL_REPO_URL=https://github.com/com-lihaoyi/mill"

rem %~1% removes surrounding quotes
if [%~1%]==[--mill-version] (
    rem shift command doesn't work within parentheses
    if not [%~2%]==[] (
        set MILL_VERSION=%~2%
        set "STRIP_VERSION_PARAMS=true"
    ) else (
        echo You specified --mill-version without a version.
        echo Please provide a version that matches one provided on
        echo %MILL_REPO_URL%/releases
        exit /b 1
    )
)

if [!MILL_VERSION!]==[] (
  if exist .mill-version (
      set /p MILL_VERSION=<.mill-version
  )
)

if [!MILL_VERSION!]==[] (
    set MILL_VERSION=%DEFAULT_MILL_VERSION%
)

set MILL_REPO_URL=

set MILL_PARAMS=%*

if defined STRIP_VERSION_PARAMS (
    for /f "tokens=1-2*" %%a in ("%*") do (
        rem strip %%a - It's the "--mill-version" option.
        rem strip %%b - it's the version number that comes after the option.
        rem keep  %%c - It's the remaining options.
        set MILL_PARAMS=%%c
    )
)

if "%1" == "-i" set _I_=true
if "%1" == "--interactive" set _I_=true
if defined _I_ (
  set MILL_APP_NAME="mill-interactive"
) else (
  set MILL_APP_NAME="mill"
)

set "mill_cs_opts="

set "mill_jvm_opts_file=.mill-jvm-opts"
if not "%MILL_JVM_OPTS_PATH%"=="" set "mill_jvm_opts_file=%MILL_JVM_OPTS_PATH%"
if exist %mill_jvm_opts_file% (
  for /f "delims=" %%G in (%mill_jvm_opts_file%) do (
    set line=%%G
    if "!line:~0,2!"=="-X" set "mill_cs_opts=!mill_cs_opts! --java-opt !line!"
  )
)

set "mill_cs_opts_file=.mill-cs-opts"
if not "%MILL_CS_OPTS_PATH%"=="" set "mill_cs_opts_file=%MILL_CS_OPTS_PATH%"
if exist %mill_cs_opts_file% (
  for /f "delims=" %%G in (%mill_cs_opts_file%) do (
    set line=%%G
    if not "!line:~0,1!"=="#" set "mill_cs_opts=!mill_cs_opts! !line!"
  )
)

rem Disabled for now, having issues with 'tar', that seems to use a Git Bash-provided (?)
rem binary rather than a Windows-provided one, when run from Git Bash. The latter accepts
rem zip files, while the former doesn't.

rem Adapted from the Mill 0.10.0-M5 assembly header
rem set "CS_DOWNLOAD_PATH=out"
rem set "CS_VERSION=2.1.0-M2"
rem set "CS=%CS_DOWNLOAD_PATH%\cs-%CS_VERSION%.exe"
rem set "DOWNLOAD_URL=https://github.com/coursier/coursier/releases/download/v%CS_VERSION%/cs-x86_64-pc-win32.zip"
rem set "DOWNLOAD_FILE=%CS_DOWNLOAD_PATH%\cs-%CS_VERSION%.zip"
rem if not exist "%CS_DOWNLOAD_PATH%" mkdir "%CS_DOWNLOAD_PATH%"
rem if not exist "%CS%" (
rem   rem curl is bundled with recent Windows 10
rem   rem but I don't think we can expect all the users to have it in 2019
rem   where /Q curl
rem   if %ERRORLEVEL% EQU 0 (
rem       curl -f -L "!DOWNLOAD_URL!" -o "!DOWNLOAD_FILE!"
rem   ) else (
rem       rem bitsadmin seems to be available on Windows 7
rem       rem without /dynamic, github returns 403
rem       rem bitsadmin is sometimes needlessly slow but it looks better with /priority foreground
rem       bitsadmin /transfer millDownloadJob /dynamic /priority foreground "!DOWNLOAD_URL!" "!DOWNLOAD_FILE!"
rem   )
rem   if not exist "!DOWNLOAD_FILE!" (
rem       echo Could not download cs %CS_VERSION% 1>&2
rem       exit /b 1 REM Seems this doesn't actually make the script exit with an error code…
rem   )
rem
rem   tar -xf "!DOWNLOAD_FILE!"
rem   move /y "cs-x86_64-pc-win32.exe" "%CS%"
rem )

echo Using system found cs command. If anything goes wrong, ensure it's at least coursier 2.1.0-M5-5-g2cb552ea9. 1>&2

cs launch --shared org.scala-lang:scala-library "%MILL_APP_NAME%:%MILL_VERSION%" !mill_cs_opts! -- %MILL_PARAMS%
