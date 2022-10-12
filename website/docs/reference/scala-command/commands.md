---
title: Commands
sidebar_position: 3
---

**This document describes as scala-cli behaves if run as `scala` command. See more information in [SIP-46](https://github.com/scala/improvement-proposals/pull/46)**


# `scala` commands

This document is a specification of `scala` runner.
For now it uses documentation specific to Scala CLI but at some point it may be refactor to provide more abstract documentation.
Documentation is splitted into sections in the spirit of RFC keywords (`MUST`, `SHOULD`, `NICE TO HAVE`) including `IMPLEMENTATION` category,
that is reserved for commands that needs to be present for Scala CLI to work properly but should not be a part of an official API.

## MUST have commands:

### compile

Compile Scala code

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [compile](./cli-options.md#compile-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [directories](./cli-options.md#directories-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

### doc

Generate Scaladoc documentation

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [directories](./cli-options.md#directories-options), [doc](./cli-options.md#doc-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

### help

Print help message

Accepts option groups: [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### repl

Aliases: `console`

Fire-up a Scala REPL

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [directories](./cli-options.md#directories-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [java](./cli-options.md#java-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [repl](./cli-options.md#repl-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

### run

Compile and run Scala code.

To pass arguments to the application, just add them after `--`, like:

```sh
scala-cli MyApp.scala -- first-arg second-arg
```

Accepts option groups: [benchmarking](./cli-options.md#benchmarking-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [directories](./cli-options.md#directories-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [java](./cli-options.md#java-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [main class](./cli-options.md#main-class-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [run](./cli-options.md#run-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

### shebang

Like `run`, but more handy from shebang scripts

This command is equivalent to `run`, but it changes the way
Scala CLI parses its command-line arguments in order to be compatible
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



Accepts option groups: [benchmarking](./cli-options.md#benchmarking-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [directories](./cli-options.md#directories-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [java](./cli-options.md#java-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [main class](./cli-options.md#main-class-options), [markdown](./cli-options.md#markdown-options), [python](./cli-options.md#python-options), [run](./cli-options.md#run-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

## SHOULD have commands:

### fmt

Aliases: `format`, `scalafmt`

Format Scala code

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [directories](./cli-options.md#directories-options), [fmt](./cli-options.md#fmt-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

### test

Compile and test Scala code

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [cross](./cli-options.md#cross-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [directories](./cli-options.md#directories-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [java](./cli-options.md#java-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [test](./cli-options.md#test-options), [verbosity](./cli-options.md#verbosity-options), [watch](./cli-options.md#watch-options), [workspace](./cli-options.md#workspace-options)

### version

Print version

Accepts option groups: [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options), [version](./cli-options.md#version-options)

## Implementation-specific commands

Commands that are used within Scala CLI that should be a part of the `scala` command but shouldn't be a part of specification.

### about

Print details about this application

Accepts option groups: [about](./cli-options.md#about-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### bsp

Start BSP server

Accepts option groups: [bsp](./cli-options.md#bsp-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [directories](./cli-options.md#directories-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

### clean

Clean the workspace

Accepts option groups: [bsp file](./cli-options.md#bsp-file-options), [directories](./cli-options.md#directories-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

### doctor

Print details about this application

Accepts option groups: [doctor](./cli-options.md#doctor-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### install completions

Aliases: `install-completions`

Installs completions into your shell

Accepts option groups: [directories](./cli-options.md#directories-options), [install completions](./cli-options.md#install-completions-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### install-home

Install Scala CLI in a sub-directory of the home directory

Accepts option groups: [install home](./cli-options.md#install-home-options), [logging](./cli-options.md#logging-options), [verbosity](./cli-options.md#verbosity-options)

### setup-ide

Generate a BSP file that you can import into your IDE

Accepts option groups: [bsp file](./cli-options.md#bsp-file-options), [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [debug](./cli-options.md#debug-options), [dependency](./cli-options.md#dependency-options), [directories](./cli-options.md#directories-options), [help group](./cli-options.md#help-group-options), [input](./cli-options.md#input-options), [jvm](./cli-options.md#jvm-options), [logging](./cli-options.md#logging-options), [markdown](./cli-options.md#markdown-options), [Scala.js](./cli-options.md#scalajs-options), [Scala Native](./cli-options.md#scala-native-options), [scalac](./cli-options.md#scalac-options), [scalac extra](./cli-options.md#scalac-extra-options), [setup IDE](./cli-options.md#setup-ide-options), [shared](./cli-options.md#shared-options), [snippet](./cli-options.md#snippet-options), [verbosity](./cli-options.md#verbosity-options), [workspace](./cli-options.md#workspace-options)

### uninstall

Uninstall scala-cli - only works when installed by the installation script

Accepts option groups: [compilation server](./cli-options.md#compilation-server-options), [coursier](./cli-options.md#coursier-options), [directories](./cli-options.md#directories-options), [logging](./cli-options.md#logging-options), [uninstall](./cli-options.md#uninstall-options), [uninstall completions](./cli-options.md#uninstall-completions-options), [verbosity](./cli-options.md#verbosity-options)

### uninstall completions

Aliases: `uninstall-completions`

Uninstalls completions from your shell

Accepts option groups: [logging](./cli-options.md#logging-options), [uninstall completions](./cli-options.md#uninstall-completions-options), [verbosity](./cli-options.md#verbosity-options)

### update

Update scala-cli - only works when installed by the installation script

Accepts option groups: [logging](./cli-options.md#logging-options), [update](./cli-options.md#update-options), [verbosity](./cli-options.md#verbosity-options)

