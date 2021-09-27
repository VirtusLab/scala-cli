---
title: Command-line options
sidebar_position: 1
---

## Add path options

Available in commands:
- [`add-path`](./commands#add-path)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--quiet`

Aliases: `-q`

#### `--title`

## Benchmarking options

Available in commands:
- [`run`](./commands#run)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--jmh`

Run JMH benchmarks

#### `--jmh-version`

Set JMH version

## Bloop start options

Available in commands:
- [`bloop start`](./commands#bloop-start)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--force`

Aliases: `-f`

## Compilation server options

Available in commands:
- [`bloop exit`](./commands#bloop-exit)
- [`bloop start`](./commands#bloop-start)
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--bloop-bsp-protocol`

Protocol to use to open a BSP connection with Bloop (ignored on Windows for now)

#### `--bloop-bsp-socket`

Socket file to use to open a BSP connection with Bloop (on Windows, pipe name like "\\.\pipe\…")

#### `--bloop-host`

Host the compilation server should bind to

#### `--bloop-port`

Port the compilation server should bind to (pass -1 to pick a random port)

#### `--bloop-bsp-timeout`

Maximum duration to wait for BSP connection to be opened

#### `--bloop-bsp-check-period`

Duration between checks of the BSP connection state

#### `--bloop-startup-timeout`

Maximum duration to wait for compilation server to start up

## Compile options

Available in commands:
- [`compile`](./commands#compile)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--class-path`

Aliases: `-p`, `--classpath`

Print resulting class path

## Compile cross options

Available in commands:
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--cross`

Cross-compile sources

## Coursier options

Available in commands:
- [`bloop start`](./commands#bloop-start)
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--ttl`

Specify a TTL for changing dependencies, such as snapshots

## Cross options

Available in commands:
- [`compile`](./commands#compile)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--cross`

## Dependency options

Available in commands:
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--dependency`

Aliases: `--dep`, `-d`

Add dependencies

#### `--repository`

Aliases: `--repo`, `-r`

Add repositories

#### `--compiler-plugin`

Aliases: `-P`, `--plugin`

Add compiler plugins dependencies

## Directories options

Available in commands:
- [`bloop exit`](./commands#bloop-exit)
- [`bloop start`](./commands#bloop-start)
- [`bsp`](./commands#bsp)
- [`clean`](./commands#clean)
- [`compile`](./commands#compile)
- [`directories`](./commands#directories)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`install completions`](./commands#install-completions)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--home-directory`

Aliases: `--home`

## Export options

Available in commands:
- [`export`](./commands#export)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--sbt`

#### `--mill`

#### `--sbt-setting`

Aliases: `--setting`

#### `--output`

Aliases: `-o`

## Fmt options

Available in commands:
- [`fmt`](./commands#fmt)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--check`

Check that sources are well formatted

#### `--os-arch-suffix`

#### `--scalafmt-tag`

#### `--scalafmt-git-hub-org-name`

#### `--scalafmt-extension`

#### `--scalafmt-launcher`

## Help options

Available in commands:
- [`about`](./commands#about)
- [`add-path`](./commands#add-path)
- [`bloop exit`](./commands#bloop-exit)
- [`bloop start`](./commands#bloop-start)
- [`bsp`](./commands#bsp)
- [`clean`](./commands#clean)
- [`compile`](./commands#compile)
- [`directories`](./commands#directories)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`install completions`](./commands#install-completions)
- [`install-home`](./commands#install-home)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--usage`

Print usage and exit

#### `--help`

Aliases: `-h`, `-help`

Print help message and exit

#### `--help-full`

Aliases: `--full-help`

Print help message, including hidden options, and exit

## Install completions options

Available in commands:
- [`install completions`](./commands#install-completions)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--format`

Aliases: `--shell`

#### `--rc-file`

#### `--directory`

#### `--banner`

#### `--name`

#### `--env`

## Install home options

Available in commands:
- [`install-home`](./commands#install-home)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--scala-cli-binary-path`

#### `--force`

Aliases: `-f`

Overwrite scala-cli if exists

## Java options

Available in commands:
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--java-opt`

Aliases: `-J`

Set Java options, such as -Xmx1g

#### `--java-prop`

Set Java properties

## Jvm options

Available in commands:
- [`bloop start`](./commands#bloop-start)
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--java-home`

Set Java home

#### `--jvm`

Aliases: `-j`

Use a specific JVM, such as 14, adopt:11, or graalvm:21, or system

#### `--jvm-index`

JVM index URL

#### `--jvm-index-os`

Operating system to use when looking up in the JVM index

#### `--jvm-index-arch`

CPU architecture to use when looking up in the JVM index

## Logging options

Available in commands:
- [`bloop exit`](./commands#bloop-exit)
- [`bloop start`](./commands#bloop-start)
- [`bsp`](./commands#bsp)
- [`clean`](./commands#clean)
- [`compile`](./commands#compile)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`install completions`](./commands#install-completions)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--verbose`

Aliases: `-v`

Increase verbosity (can be specified multiple times)

#### `--quiet`

Aliases: `-q`

Decrease verbosity

#### `--progress`

Use progress bars

## Main class options

Available in commands:
- [`export`](./commands#export)
- [`package`](./commands#package)
- [`run`](./commands#run)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--main-class`

Aliases: `-M`

Specify which main class to run

## Metabrowse options

Available in commands:
- [`browse` / `metabrowse`](./commands#browse)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--add-rt-jar`

Download and add rt.jar in the class path

#### `--host`

Aliases: `-H`

Bind to host

#### `--port`

Aliases: `-p`

Bind to port

#### `--os-arch-suffix`

#### `--metabrowse-tag`

#### `--metabrowse-git-hub-org-name`

#### `--metabrowse-extension`

#### `--metabrowse-launcher`

#### `--metabrowse-dialect`

## Package options

Available in commands:
- [`package`](./commands#package)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--output`

Aliases: `-o`

Set destination path

#### `--force`

Aliases: `-f`

Overwrite destination file if it exists

#### `--library`

Generate a library JAR rather than an executable JAR

#### `--assembly`

Generate an assembly JAR

#### `--standalone`

Package standalone JARs

#### `--deb`

Build debian package, available only on linux

#### `--dmg`

Build dmg package, available only on macOS

#### `--rpm`

Build rpm package, available only on linux

#### `--msi`

Build msi package, available only on windows

#### `--pkg`

Build pkg package, available only on macOS

#### `--docker`

Build docker image

## Packager options

Available in commands:
- [`package`](./commands#package)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--version`

The version of generated package

#### `--logo-path`

Path to application logo in png format, it will be used to generate icon and banner/dialog in msi installer

#### `--launcher-app`

Set launcher app name which will be linked to PATH

#### `--description`

#### `--maintainer`

Aliases: `-m`

It should contains names and email addresses of co-maintainers of the package

#### `--debian-conflicts`

The list of debian package that this package is absolute incompatibility

#### `--debian-dependencies`

The list of debian package that this package depends on

#### `--deb-architecture`

Architecture that are supported by the repository, default: all

#### `--identifier`

CF Bundle Identifier

#### `--license`

License that are supported by the repository - list of licenses https://fedoraproject.org/wiki/Licensing:Main?rd=Licensing

#### `--release`

The number of times this version of the software was released, default: 1

#### `--rpm-architecture`

Architecture that are supported by the repository, default: noarch

#### `--license-path`

Path to license file

#### `--product-name`

Name of product, default: Scala packager

#### `--exit-dialog`

Text will be displayed on exit dialog

#### `--suppress-validation`

Suppress Wix ICE validation (required for users that are neither interactive, not local administrators)

#### `--extra-config`

Path to extra WIX config content

#### `--is64-bits`

Aliases: `--64`

Whether a 64-bit executable is getting packaged

#### `--installer-version`

WIX installer version

#### `--docker-from`

Building the container from base image

#### `--docker-image-registry`

The image registry, if will be empty it will be used default registry

#### `--docker-image-repository`

The image repository

#### `--docker-image-tag`

The image tag, the default tag is latest

## Repl options

Available in commands:
- [`console` / `repl`](./commands#console)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--ammonite`

Aliases: `-A`, `--amm`

Use Ammonite rather than the default Scala REPL

#### `--ammonite-version`

Aliases: `--ammonite-ver`

Set Ammonite version

#### `--ammonite-arg`

Aliases: `-a`

#### `--repl-dry-run`

Don't actually run the REPL, only fetch it

## Scala.JS options

Available in commands:
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--js`

Enable Scala.JS

#### `--js-version`

#### `--js-mode`

#### `--js-module-kind`

#### `--js-check-ir`

#### `--js-emit-source-maps`

#### `--js-dom`

## Scala Native options

Available in commands:
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--native`

Enable Scala Native

#### `--native-version`

#### `--native-mode`

#### `--native-gc`

#### `--native-clang`

#### `--native-clangpp`

#### `--native-linking`

#### `--native-linking-defaults`

#### `--native-compile`

#### `--native-compile-defaults`

## Scalac options

Available in commands:
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--scalac-option`

Aliases: `--scala-opt`, `-O`, `-P`, `-W`, `-g`, `-X`, `-language`, `-Y`, `-V`, `-target`, `-opt`

Add scalac option

## Setup IDE options

Available in commands:
- [`setup-ide`](./commands#setup-ide)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--bsp-directory`

Aliases: `--bsp-dir`

#### `--bsp-name`

Aliases: `--name`

#### `--charset`

## Shared options

Available in commands:
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
- [`export`](./commands#export)
- [`fmt`](./commands#fmt)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--scala-version`

Aliases: `--scala`, `-S`

Set Scala version

#### `--scala-binary-version`

Aliases: `--scala-binary`, `--scala-bin`, `-B`

Set Scala binary version

#### `--extra-jars`

Aliases: `--jar`, `--jars`, `--extra-jar`

Add extra JARs in the class path

#### `--extra-compile-only-jars`

Aliases: `--compile-only-jar`, `--compile-only-jars`, `--extra-compile-only-jar`

Add extra JARs in the class path during compilation only

#### `--extra-source-jars`

Aliases: `--source-jar`, `--source-jars`, `--extra-source-jar`

Add extra source JARs

#### `--class-wrap`

#### `--scala-library`

#### `--java`

#### `--runner`

#### `--semantic-db`

Generate SemanticDBs

#### `--add-stubs`

#### `--default-forbidden-directories`

#### `--forbid`

## Test options

Available in commands:
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--test-framework`

Test framework to use to run tests

## Watch options

Available in commands:
- [`compile`](./commands#compile)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--watch`

Aliases: `-w`

Watch sources for changes

