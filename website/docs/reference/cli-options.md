---
title: Command-line options
sidebar_position: 1
---

This is a summary of options that are available for each subcommand of the `scala-cli` command.

## Add path options

Available in commands:
- [`add-path`](./commands.md#add-path)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--quiet`

Aliases: `-q`

#### `--title`

## Benchmarking options

Available in commands:
- [`run`](./commands.md#run)
- [`shebang`](./commands.md#shebang)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--jmh`

Run JMH benchmarks

#### `--jmh-version`

Set JMH version

## Bloop start options

Available in commands:
- [`bloop start`](./commands.md#bloop-start)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--force`

Aliases: `-f`

## Bsp options

Available in commands:
- [`bsp`](./commands.md#bsp)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--json-options`

Command-line options JSON file

## Bsp file options

Available in commands:
- [`clean`](./commands.md#clean)
- [`setup-ide`](./commands.md#setup-ide)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--bsp-directory`

Aliases: `--bsp-dir`

Custom BSP configuration location

#### `--bsp-name`

Aliases: `--name`

Name of BSP

## Compilation server options

Available in commands:
- [`bloop exit`](./commands.md#bloop-exit)
- [`bloop start`](./commands.md#bloop-start)
- [`bsp`](./commands.md#bsp)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--bloop-bsp-protocol`

Protocol to use to open a BSP connection with Bloop

#### `--bloop-bsp-socket`

Socket file to use to open a BSP connection with Bloop (on Windows, a pipe name like "`\\.\pipe\…`")

#### `--bloop-host`

Host the compilation server should bind to

#### `--bloop-port`

Port the compilation server should bind to (pass `-1` to pick a random port)

#### `--bloop-daemon-dir`

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

#### `--bloop-version`

If Bloop isn't already running, the version we should start

#### `--bloop-bsp-timeout`

Maximum duration to wait for the BSP connection to be opened

#### `--bloop-bsp-check-period`

Duration between checks of the BSP connection state

#### `--bloop-startup-timeout`

Maximum duration to wait for the compilation server to start up

#### `--bloop-default-java-opts`

Include default JVM options for Bloop

#### `--bloop-java-opt`

#### `--bloop-global-options-file`

Bloop global options file

#### `--bloop-jvm`

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

#### `--bloop-working-dir`

Working directory for Bloop, if it needs to be started

#### `--server`

Enable / disable compilation server

## Compile options

Available in commands:
- [`compile`](./commands.md#compile)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--class-path`

Aliases: `-p`, `--classpath`

Print the resulting class path

#### `--test`

Compile test scope

## Compile cross options

Available in commands:
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--cross`

Aliases: `-X`

Cross-compile sources

## Coursier options

Available in commands:
- [`bloop exit`](./commands.md#bloop-exit)
- [`bloop start`](./commands.md#bloop-start)
- [`bsp`](./commands.md#bsp)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--ttl`

Specify a TTL for changing dependencies, such as snapshots

#### `--cache`

Set the coursier cache location

## Cross options

Available in commands:
- [`compile`](./commands.md#compile)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--cross`

Aliases: `-X`

## Dependency options

Available in commands:
- [`bsp`](./commands.md#bsp)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--dependency`

Aliases: `--dep`, `-d`

Add dependencies

#### `--repository`

Aliases: `--repo`, `-r`

Add repositories

#### `--compiler-plugin`

Aliases: `-P`, `--plugin`

Add compiler plugin dependencies

## Directories options

Available in commands:
- [`bloop exit`](./commands.md#bloop-exit)
- [`bloop start`](./commands.md#bloop-start)
- [`bsp`](./commands.md#bsp)
- [`clean`](./commands.md#clean)
- [`compile`](./commands.md#compile)
- [`directories`](./commands.md#directories)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`install completions` / `install-completions`](./commands.md#install-completions)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--home-directory`

Aliases: `--home`

## Export options

Available in commands:
- [`export`](./commands.md#export)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--sbt`

#### `--mill`

#### `--sbt-setting`

Aliases: `--setting`

#### `--sbt-version`

#### `--output`

Aliases: `-o`

## Fmt options

Available in commands:
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--check`

Check that sources are well formatted

#### `--os-arch-suffix`

#### `--scalafmt-tag`

#### `--scalafmt-github-org-name`

#### `--scalafmt-extension`

#### `--scalafmt-launcher`

#### `--scalafmt-arg`

Aliases: `-F`

#### `--dialect`

## Help options

Available in commands:
- [`about`](./commands.md#about)
- [`add-path`](./commands.md#add-path)
- [`bloop exit`](./commands.md#bloop-exit)
- [`bloop start`](./commands.md#bloop-start)
- [`bsp`](./commands.md#bsp)
- [`clean`](./commands.md#clean)
- [`compile`](./commands.md#compile)
- [`directories`](./commands.md#directories)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`help`](./commands.md#help)
- [`install completions` / `install-completions`](./commands.md#install-completions)
- [`install-home`](./commands.md#install-home)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)
- [`update`](./commands.md#update)
- [`version`](./commands.md#version)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--usage`

Print usage and exit

#### `--help`

Aliases: `-h`, `-help`

Print help message and exit

#### `--help-full`

Aliases: `--full-help`

Print help message, including hidden options, and exit

## Help group options

Available in commands:
- [`bsp`](./commands.md#bsp)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--help-js`

Show options for ScalaJS

#### `--help-native`

Show options for ScalaNative

## Install completions options

Available in commands:
- [`install completions` / `install-completions`](./commands.md#install-completions)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--format`

Aliases: `--shell`

Name of the shell, either zsh or bash

#### `--rc-file`

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

#### `--output`

Aliases: `-o`

Completions output directory

#### `--banner`

Custom banner in comment placed in rc file

#### `--name`

Custom completions name

#### `--env`

Print completions to stdout

## Install home options

Available in commands:
- [`install-home`](./commands.md#install-home)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--scala-cli-binary-path`

#### `--force`

Aliases: `-f`

Overwrite `scala-cli`, if it exists

#### `--binary-name`

Binary name

#### `--env`

Print the update to `env` variable

#### `--bin-dir`

Binary directory

## Java options

Available in commands:
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--java-opt`

Aliases: `-J`

Set Java options, such as `-Xmx1g`

#### `--java-prop`

Set Java properties

## Jvm options

Available in commands:
- [`bloop start`](./commands.md#bloop-start)
- [`bsp`](./commands.md#bsp)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--java-home`

Set the Java home directory

#### `--jvm`

Aliases: `-j`

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

#### `--jvm-index`

JVM index URL

#### `--jvm-index-os`

Operating system to use when looking up in the JVM index

#### `--jvm-index-arch`

CPU architecture to use when looking up in the JVM index

#### `--javac-plugin`

Javac plugin dependencies or files

#### `--javac-option`

Aliases: `--javac-opt`

Javac options

#### `--bsp-debug-port`

Port for BSP debugging

## Logging options

Available in commands:
- [`bloop exit`](./commands.md#bloop-exit)
- [`bloop start`](./commands.md#bloop-start)
- [`bsp`](./commands.md#bsp)
- [`clean`](./commands.md#clean)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`install completions` / `install-completions`](./commands.md#install-completions)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--quiet`

Aliases: `-q`

Decrease verbosity

#### `--progress`

Use progress bars

## Main class options

Available in commands:
- [`export`](./commands.md#export)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`run`](./commands.md#run)
- [`shebang`](./commands.md#shebang)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--main-class`

Aliases: `-M`

Specify which main class to run

## Metabrowse options

Available in commands:
- [`browse` / `metabrowse`](./commands.md#browse)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--add-rt-jar`

Download and add `rt.jar` in the class path

#### `--host`

Aliases: `-H`

Bind to host

#### `--port`

Aliases: `-p`

Bind to port

#### `--os-arch-suffix`

#### `--metabrowse-tag`

#### `--metabrowse-github-org-name`

#### `--metabrowse-extension`

#### `--metabrowse-launcher`

#### `--metabrowse-dialect`

## Package options

Available in commands:
- [`package`](./commands.md#package)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--output`

Aliases: `-o`

Set the destination path

#### `--force`

Aliases: `-f`

Overwrite the destination file, if it exists

#### `--library`

Generate a library JAR rather than an executable JAR

#### `--source`

Generate a source JAR rather than an executable JAR

#### `--doc`

Aliases: `--scaladoc`, `--javadoc`

Generate a scaladoc JAR rather than an executable JAR

#### `--assembly`

Generate an assembly JAR

#### `--standalone`

Package standalone JARs

#### `--deb`

Build Debian package, available only on Linux

#### `--dmg`

Build dmg package, available only on macOS

#### `--rpm`

Build rpm package, available only on Linux

#### `--msi`

Build msi package, available only on Windows

#### `--pkg`

Build pkg package, available only on macOS

#### `--docker`

Build Docker image

#### `--default-scaladoc-options`

Aliases: `--default-scaladoc-opts`

Use default scaladoc options

## Packager options

Available in commands:
- [`package`](./commands.md#package)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--version`

Set the version of the generated package

#### `--logo-path`

Path to application logo in PNG format, it will be used to generate icon and banner/dialog in msi installer

#### `--launcher-app`

Set launcher app name, which will be linked to the PATH

#### `--description`

#### `--maintainer`

Aliases: `-m`

This should contain names and email addresses of co-maintainers of the package

#### `--debian-conflicts`

The list of Debian package that this package is not compatible with

#### `--debian-dependencies`

The list of Debian packages that this package depends on

#### `--deb-architecture`

Architectures that are supported by the repository (default: all)

#### `--identifier`

CF Bundle Identifier

#### `--license`

Licenses that are supported by the repository (list of licenses: https://fedoraproject.org/wiki/Licensing:Main?rd=Licensing)

#### `--release`

The number of times this version of the software was released (default: 1)

#### `--rpm-architecture`

Architectures that are supported by the repository (default: noarch)

#### `--license-path`

Path to the license file

#### `--product-name`

Name of product (default: Scala packager)

#### `--exit-dialog`

Text that will be displayed on the exit dialog

#### `--suppress-validation`

Suppress Wix ICE validation (required for users that are neither interactive, not local administrators)

#### `--extra-config`

Path to extra WIX configuration content

#### `--is64-bits`

Aliases: `--64`

Whether a 64-bit executable is being packaged

#### `--installer-version`

WIX installer version

#### `--docker-from`

Building the container from base image

#### `--docker-image-registry`

The image registry; if empty, it will use the default registry

#### `--docker-image-repository`

The image repository

#### `--docker-image-tag`

The image tag; the default tag is `latest`

## Publish options

Available in commands:
- [`publish`](./commands.md#publish)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--working-dir`

Directory where temporary files for publishing should be written

#### `--organization`

Organization to publish artifacts under

#### `--module-name`

Module name to publish artifacts as

#### `--version`

Version to publish artifacts as

#### `--url`

URL to put in publishing metadata

#### `--license`

License to put in publishing metadata

#### `--vcs`

VCS information to put in publishing metadata

#### `--description`

Description to put in publishing metadata

#### `--developer`

Developer(s) to add in publishing metadata, like "alex|Alex|https://alex.info" or "alex|Alex|https://alex.info|alex@alex.me"

#### `--scala-version-suffix`

Scala version suffix to append to the module name, like "_2.13" or "_3"

#### `--scala-platform-suffix`

Scala platform suffix to append to the module name, like "_sjs1" or "_native0.4"

#### `--publish-repository`

Aliases: `-R`, `--publish-repo`

Repository to publish to

#### `--sources`

Whether to build and publish source JARs

## Repl options

Available in commands:
- [`console` / `repl`](./commands.md#console)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--ammonite`

Aliases: `-A`, `--amm`

Use Ammonite (instead of the default Scala REPL)

#### `--ammonite-version`

Aliases: `--ammonite-ver`

Set the Ammonite version

#### `--ammonite-arg`

Aliases: `-a`

#### `--repl-dry-run`

Don't actually run the REPL, just fetch it

## Scala.JS options

Available in commands:
- [`bsp`](./commands.md#bsp)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--js`

Enable Scala.JS. To show more options for Scala.Js pass `--help-js`

#### `--js-version`

The Scala JS version

#### `--js-mode`

The Scala JS mode, either `dev` or `release`

#### `--js-module-kind`

The Scala JS module kind: commonjs/common, esmodule/es, nomodule/none

#### `--js-check-ir`

#### `--js-emit-source-maps`

Emit source maps

#### `--js-source-maps-path`

Set the destination path of source maps

#### `--js-dom`

Enable jsdom

#### `--js-header`

A header that will be added at the top of generated .js files

#### `--js-allow-big-ints-for-longs`

Primitive Longs *may* be compiled as primitive JavaScript bigints

#### `--js-avoid-classes`

Avoid class'es when using functions and prototypes has the same observable semantics.

#### `--js-avoid-lets-and-consts`

Avoid lets and consts when using vars has the same observable semantics.

#### `--js-module-split-style`

The Scala JS module split style: fewestmodules, smallestmodules

#### `--js-es-version`

The Scala JS ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

## Scala Native options

Available in commands:
- [`bsp`](./commands.md#bsp)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--native`

Enable Scala Native. To show more options for Scala Native pass `--help-native`

#### `--native-version`

Set the Scala Native version

#### `--native-mode`

Set Scala Native compilation mode

#### `--native-gc`

Set the Scala Native garbage collector

#### `--native-clang`

Path to the Clang command

#### `--native-clangpp`

Path to the Clang++ command

#### `--native-linking`

Extra options passed to `clang` verbatim during linking

#### `--native-linking-defaults`

Use default linking settings

#### `--native-compile`

List of compile options

#### `--native-compile-defaults`

Use default compile options

## Scalac options

Available in commands:
- [`bsp`](./commands.md#bsp)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--scalac-option`

Aliases: `--scala-opt`, `-O`

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

## Setup IDE options

Available in commands:
- [`setup-ide`](./commands.md#setup-ide)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--charset`

## Shared options

Available in commands:
- [`bsp`](./commands.md#bsp)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--scala-version`

Aliases: `--scala`, `-S`

Set the Scala version

#### `--scala-binary-version`

Aliases: `--scala-binary`, `--scala-bin`, `-B`

Set the Scala binary version

#### `--extra-jars`

Aliases: `--jar`, `--jars`, `--extra-jar`

Add extra JARs in the class path

#### `--extra-compile-only-jars`

Aliases: `--compile-only-jar`, `--compile-only-jars`, `--extra-compile-only-jar`

Add extra JARs in the class path, during compilation only

#### `--extra-source-jars`

Aliases: `--source-jar`, `--source-jars`, `--extra-source-jar`

Add extra source JARs

#### `--resource-dirs`

Aliases: `--resource-dir`

Add a resource directory

#### `--scala-library`

#### `--java`

#### `--runner`

#### `--semantic-db`

Generate SemanticDBs

#### `--add-stubs`

#### `--default-forbidden-directories`

#### `--forbid`

#### `--strict-bloop-json-check`

## Test options

Available in commands:
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--test-framework`

Name of the test framework's runner class to use while running tests

#### `--require-tests`

Fail if no test suites were run

## Update options

Available in commands:
- [`update`](./commands.md#update)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--binary-name`

Binary name

#### `--bin-dir`

Binary directory

#### `--force`

Aliases: `-f`

Force update scala-cli if is outdated

#### `--is-internal-run`

## Verbosity options

Available in commands:
- [`about`](./commands.md#about)
- [`add-path`](./commands.md#add-path)
- [`bloop exit`](./commands.md#bloop-exit)
- [`bloop start`](./commands.md#bloop-start)
- [`bsp`](./commands.md#bsp)
- [`clean`](./commands.md#clean)
- [`compile`](./commands.md#compile)
- [`directories`](./commands.md#directories)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`install completions` / `install-completions`](./commands.md#install-completions)
- [`install-home`](./commands.md#install-home)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)
- [`update`](./commands.md#update)
- [`version`](./commands.md#version)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--verbose`

Aliases: `-v`

Increase verbosity (can be specified multiple times)

## Watch options

Available in commands:
- [`compile`](./commands.md#compile)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--watch`

Aliases: `-w`

Watch source files for changes

## Workspace options

Available in commands:
- [`bsp`](./commands.md#bsp)
- [`clean`](./commands.md#clean)
- [`compile`](./commands.md#compile)
- [`export`](./commands.md#export)
- [`fmt` / `format` / `scalafmt`](./commands.md#fmt)
- [`browse` / `metabrowse`](./commands.md#browse)
- [`package`](./commands.md#package)
- [`publish`](./commands.md#publish)
- [`console` / `repl`](./commands.md#console)
- [`run`](./commands.md#run)
- [`setup-ide`](./commands.md#setup-ide)
- [`shebang`](./commands.md#shebang)
- [`test`](./commands.md#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--workspace`

Directory where .scala-build is written

