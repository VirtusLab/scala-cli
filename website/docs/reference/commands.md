---
title: Commands
sidebar_position: 3
---

## `about`

Print details about this application

Accepts options:
- [verbosity](./cli-options.md#verbosity-options)

## `clean`

Clean the workspace

Accepts options:
- [bsp file](./cli-options.md#bsp-file-options)
- [directories](./cli-options.md#directories-options)
- [logging](./cli-options.md#logging-options)
- [verbosity](./cli-options.md#verbosity-options)

## `compile`

Compile Scala code

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [compile](./cli-options.md#compile-options)
- [coursier](./cli-options.md#coursier-options)
- [cross](./cli-options.md#cross-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [help group](./cli-options.md#help-group-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)
- [watch](./cli-options.md#watch-options)

## `export`

Export current project to sbt or Mill

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [export](./cli-options.md#export-options)
- [help group](./cli-options.md#help-group-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [main class](./cli-options.md#main-class-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)

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
- [help group](./cli-options.md#help-group-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)

## `help`

Print help message

## `install completions`

Aliases:
- `install-completions`

Installs completions into your shell

Accepts options:
- [directories](./cli-options.md#directories-options)
- [install completions](./cli-options.md#install-completions-options)
- [logging](./cli-options.md#logging-options)
- [verbosity](./cli-options.md#verbosity-options)

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
- [help group](./cli-options.md#help-group-options)
- [java](./cli-options.md#java-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [repl](./cli-options.md#repl-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)
- [watch](./cli-options.md#watch-options)

## `package`

Compile and package Scala code

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [compile cross](./cli-options.md#compile-cross-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [help group](./cli-options.md#help-group-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [main class](./cli-options.md#main-class-options)
- [package](./cli-options.md#package-options)
- [packager](./cli-options.md#packager-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)
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
- [help group](./cli-options.md#help-group-options)
- [java](./cli-options.md#java-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [main class](./cli-options.md#main-class-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)
- [watch](./cli-options.md#watch-options)

## `setup-ide`

Generate a BSP file that you can import into your IDE

Accepts options:
- [bsp file](./cli-options.md#bsp-file-options)
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [help group](./cli-options.md#help-group-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [setup IDE](./cli-options.md#setup-ide-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)

## `shebang`

Like 'run', but more handy from shebang scripts

This command is equivalent to `run`, but it changes the way
`scala-cli` parses its command-line arguments in order to be compatible
with shebang scripts.

Normally, inputs and scala-cli options can be mixed. Program have to be specified after `--`

```sh
scala-cli [command] [scala_cli_options | input]... -- [program_arguments]...
```

Contrary, for shebang command, only a single input file can be set, all scala-cli options
have to be set before the input file, and program arguments after the input file
```sh
scala-cli shebang [scala_cli_options]... input [program_arguments]...
```

Using this, it is possible to conveniently set up Unix shebang scripts. For example:
```sh
#!/usr/bin/env -S scala-cli shebang --scala-version 2.13
println("Hello, world)
```



Accepts options:
- [benchmarking](./cli-options.md#benchmarking-options)
- [compilation server](./cli-options.md#compilation-server-options)
- [compile cross](./cli-options.md#compile-cross-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [help group](./cli-options.md#help-group-options)
- [java](./cli-options.md#java-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [main class](./cli-options.md#main-class-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)
- [watch](./cli-options.md#watch-options)

## `test`

Compile and test Scala code

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [compile cross](./cli-options.md#compile-cross-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [help group](./cli-options.md#help-group-options)
- [java](./cli-options.md#java-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [test](./cli-options.md#test-options)
- [verbosity](./cli-options.md#verbosity-options)
- [watch](./cli-options.md#watch-options)

## `update`

Update scala-cli - it works only for installation script

Accepts options:
- [update](./cli-options.md#update-options)
- [verbosity](./cli-options.md#verbosity-options)

## `version`

Print `scala-cli` version

Accepts options:
- [verbosity](./cli-options.md#verbosity-options)

## Hidden commands

### `add-path`

Accepts options:
- [add path](./cli-options.md#add-path-options)
- [verbosity](./cli-options.md#verbosity-options)

### `bloop exit`

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [directories](./cli-options.md#directories-options)
- [logging](./cli-options.md#logging-options)
- [verbosity](./cli-options.md#verbosity-options)

### `bloop start`

Accepts options:
- [bloop start](./cli-options.md#bloop-start-options)
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [directories](./cli-options.md#directories-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [verbosity](./cli-options.md#verbosity-options)

### `bsp`

Start BSP server

Accepts options:
- [bsp](./cli-options.md#bsp-options)
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [help group](./cli-options.md#help-group-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)

### `directories`

Prints directories used by `scala-cli`

Accepts options:
- [directories](./cli-options.md#directories-options)
- [verbosity](./cli-options.md#verbosity-options)

### `install-home`

Install `scala-cli` in a sub-directory of the home directory

Accepts options:
- [install home](./cli-options.md#install-home-options)
- [verbosity](./cli-options.md#verbosity-options)

### `browse`

Aliases:
- `metabrowse`

Browse Scala code and its dependencies in the browser

Accepts options:
- [compilation server](./cli-options.md#compilation-server-options)
- [coursier](./cli-options.md#coursier-options)
- [dependency](./cli-options.md#dependency-options)
- [directories](./cli-options.md#directories-options)
- [help group](./cli-options.md#help-group-options)
- [jvm](./cli-options.md#jvm-options)
- [logging](./cli-options.md#logging-options)
- [metabrowse](./cli-options.md#metabrowse-options)
- [Scala.JS](./cli-options.md#scalajs-options)
- [Scala Native](./cli-options.md#scala-native-options)
- [scalac](./cli-options.md#scalac-options)
- [shared](./cli-options.md#shared-options)
- [verbosity](./cli-options.md#verbosity-options)

