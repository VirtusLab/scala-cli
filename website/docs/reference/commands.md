---
title: Commands
sidebar_position: 3
---

## `about`

Print details about this application

## `bsp`

Start BSP server

Accepts options:
- [bsp](./cli-options.md#bsp-options)
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)

## `clean`

Clean the workspace

Accepts options:
- [bsp file](./cli-options.md#bsp-file-options)
- [directories](./cli-options.md#directories-options)
- [logging](./cli-options.md#logging-options)

## `compile`

Compile Scala code

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [compile](./cli-options.md#compile-options)
- [coursier](./cli-options.md#coursier-options)
- [cross](./cli-options.md#cross-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [watch](./cli-options.md#watch-options)

## `export`

Export current project to sbt or Mill

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [export](./cli-options.md#export-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [main class](./cli-options.md#main-class-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)

## `fmt`

Aliases:
- `format`
- `scalafmt`

Format Scala code

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [fmt](./cli-options.md#fmt-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)

## `install completions`

Aliases:
- `install-completions`

Installs completions into your shell

Accepts options:
- [directories](./cli-options.md#directories-options)
- [install completions](./cli-options.md#install-completions-options)
- [logging](./cli-options.md#logging-options)

## `browse`

Aliases:
- `metabrowse`

Browse Scala code and its dependencies in the browser

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [metabrowse](./cli-options.md#metabrowse-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)

## `console`

Aliases:
- `repl`

Fire-up a Scala REPL

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [compile cross](./cli-options.md#compile-cross-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [java](./cli-options.md#java-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [repl](./cli-options.md#repl-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [watch](./cli-options.md#watch-options)

## `package`

Compile and package Scala code

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [compile cross](./cli-options.md#compile-cross-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [main class](./cli-options.md#main-class-options)
- [package](./cli-options.md#package-options)
- [packager](./cli-options.md#packager-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [watch](./cli-options.md#watch-options)

## `run`

Compile and run Scala code.

To pass arguments to the application, just add them after `--`, like:

```sh
scala-cli MyApp.scala -- first-arg second-arg
```

Accepts options:
- [benchmarking](./cli-options.md#benchmarking-options)
- [compilation server](./cli-options.md#compilation-server-options)
- [compile cross](./cli-options.md#compile-cross-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [java](./cli-options.md#java-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [main class](./cli-options.md#main-class-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [watch](./cli-options.md#watch-options)

## `setup-ide`

Generate a BSP file that you can import into your IDE

Accepts options:
- [bsp file](./cli-options.md#bsp-file-options)
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [setup IDE](./cli-options.md#setup-ide-options)
- [shared](./cli-options.md#shared-options)

## `test`

Compile and test Scala code

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [compile cross](./cli-options.md#compile-cross-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [java](./cli-options.md#java-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [test](./cli-options.md#test-options)
- [watch](./cli-options.md#watch-options)

## `update`

Update scala-cli - it works only for installation script

Accepts options:
- [update](./cli-options.md#update-options)

## `version`

Print `scala-cli` version

## Hidden commands

### `add-path`

Accepts options:
- [add path](./cli-options.md#add-path-options)

### `bloop exit`

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [directories](./cli-options.md#directories-options)
- [logging](./cli-options.md#logging-options)

### `bloop start`

Accepts options:
- [bloop start](./cli-options.md#bloop-start-options)
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [directories](./cli-options.md#directories-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)

### `directories`

Prints directories used by `scala-cli`

Accepts options:
- [directories](./cli-options.md#directories-options)

### `install-home`

Install `scala-cli` in a sub-directory of the home directory

Accepts options:
- [install home](./cli-options.md#install-home-options)

