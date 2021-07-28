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

## Compilation server options

Available in commands:
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
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

Socket file to use to open a BSP connection with Bloop (ignored on Windows for now)

#### `--bloop-host`

Host the compilation server should bind to

#### `--bloop-port`

Port the compilation server should bind to (pass -1 to pick a random port)

## Compile options

Available in commands:
- [`compile`](./commands#compile)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--class-path`

Aliases: `-p`, `--classpath`

Print resulting class path

## Dependency options

Available in commands:
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
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

## Help options

Available in commands:
- [`about`](./commands#about)
- [`add-path`](./commands#add-path)
- [`bsp`](./commands#bsp)
- [`clean`](./commands#clean)
- [`compile`](./commands#compile)
- [`directories`](./commands#directories)
- [`install completions`](./commands#install-completions)
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

Aliases: `-h`

Print help message and exit

#### `--help-full`

Aliases: `--full-help`

Print help message, including hidden options, and exit

## Install completions options

Available in commands:
- [`install completions`](./commands#install-completions)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--format`

#### `--rc-file`

#### `--directory`

#### `--banner`

#### `--name`

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
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
- [`browse` / `metabrowse`](./commands#browse)
- [`package`](./commands#package)
- [`console` / `repl`](./commands#console)
- [`run`](./commands#run)
- [`setup-ide`](./commands#setup-ide)
- [`test`](./commands#test)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

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
- [`bsp`](./commands#bsp)
- [`clean`](./commands#clean)
- [`compile`](./commands#compile)
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

#### `--main-class`

Aliases: `-M`

Specify which main class to run

#### `--deb`

Build debian package, available only on linux

#### `--dmg`

Build dmg package, available only on centOS

#### `--rpm`

Build rpm package, available only on linux

#### `--msi`

Build msi package, available only on windows

#### `--pkg`

Build pkg package, available only on centOS

## Packager options

Available in commands:
- [`package`](./commands#package)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--version`

The version of generated package

#### `--logo-path`

Path to application logo in png format, it will be used to generate icon and banner/dialog in msi installer

#### `--launcher-app-name`

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

Architecture that are supported by the repository, default: 

#### `--license-path`

Path to license file

#### `--product-name`

Name of product, default: Scala packager

#### `--exit-dialog`

Text will be displayed on exit dialog

## Repl options

Available in commands:
- [`console` / `repl`](./commands#console)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--ammonite`

Aliases: `-A`

Set Ammonite version

#### `--ammonite-arg`

Aliases: `-a`

## Run options

Available in commands:
- [`run`](./commands#run)


<!-- Automatically generated, DO NOT EDIT MANUALLY -->

#### `--main-class`

Aliases: `-M`

Specify which main class to run

## Scala.JS options

Available in commands:
- [`bsp`](./commands#bsp)
- [`compile`](./commands#compile)
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

#### `--java-home`

Set Java home

#### `--extra-jars`

Aliases: `--jar`, `--jars`, `--extra-jar`

Add extra JARs in the class path

#### `--extra-compile-only-jars`

Aliases: `--compile-only-jar`, `--compile-only-jars`, `--extra-compile-only-jar`

Add extra JARs in the class path during compilation only

#### `--extra-source-jars`

Aliases: `--source-jar`, `--source-jars`, `--extra-source-jar`

Add extra source JARs

#### `--ttl`

Specify a TTL for changing dependencies, such as snapshots

#### `--class-wrap`

#### `--scala-library`

#### `--java`

#### `--runner`

#### `--config`

Aliases: `--conf`, `-C`

Pass configuration files

#### `--semantic-db`

Generate SemanticDBs

#### `--add-stubs`

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

