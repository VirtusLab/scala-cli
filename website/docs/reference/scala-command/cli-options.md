---
title: Command-line options
sidebar_position: 1
---

**This document describes as scala-cli behaves if run as `scala` command. See more information in [SIP-46](https://github.com/scala/improvement-proposals/pull/46)**

This is a summary of options that are available for each subcommand of the `scala-cli` command.

## Scalac options forwarding

 All options that start with:


- `-g`
- `-language`
- `-opt`
- `-P`
- `-target`
- `-V`
- `-W`
- `-X`
- `-Y`

are assumed to be Scala compiler options and will be propagated to Scala Compiler. This applies to all commands that uses compiler directly or indirectly. 


 ## Scalac options that are directly supported in scala CLI (so can be provided as is, without any prefixes etc.):

 - `-encoding`
 - `-release`
 - `-color`
 - `-nowarn`
 - `-feature`
 - `-deprecation`



## Compilation server options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--bloop-bsp-protocol`

`IMPLEMENTATION specific` per Scala Runner specification

Protocol to use to open a BSP connection with Bloop

### `--bloop-bsp-socket`

`IMPLEMENTATION specific` per Scala Runner specification

Socket file to use to open a BSP connection with Bloop

### `--bloop-host`

`IMPLEMENTATION specific` per Scala Runner specification

Host the compilation server should bind to

### `--bloop-port`

`IMPLEMENTATION specific` per Scala Runner specification

Port the compilation server should bind to (pass `-1` to pick a random port)

### `--bloop-daemon-dir`

`IMPLEMENTATION specific` per Scala Runner specification

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

### `--bloop-version`

`IMPLEMENTATION specific` per Scala Runner specification

If Bloop isn't already running, the version we should start

### `--bloop-bsp-timeout`

`IMPLEMENTATION specific` per Scala Runner specification

Maximum duration to wait for the BSP connection to be opened

### `--bloop-bsp-check-period`

`IMPLEMENTATION specific` per Scala Runner specification

Duration between checks of the BSP connection state

### `--bloop-startup-timeout`

`IMPLEMENTATION specific` per Scala Runner specification

Maximum duration to wait for the compilation server to start up

### `--bloop-default-java-opts`

`IMPLEMENTATION specific` per Scala Runner specification

Include default JVM options for Bloop

### `--bloop-java-opt`

`IMPLEMENTATION specific` per Scala Runner specification

Pass java options to use by Bloop server

### `--bloop-global-options-file`

`IMPLEMENTATION specific` per Scala Runner specification

Bloop global options file

### `--bloop-jvm`

`IMPLEMENTATION specific` per Scala Runner specification

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

### `--bloop-working-dir`

`IMPLEMENTATION specific` per Scala Runner specification

Working directory for Bloop, if it needs to be started

### `--server`

`IMPLEMENTATION specific` per Scala Runner specification

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

## Compile options

Available in commands:

[`compile`](./commands.md#compile)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--test`

`SHOULD have` per Scala Runner specification

Compile test scope

## Debug options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--debug`

`SHOULD have` per Scala Runner specification

Turn debugging on

### `--debug-port`

`SHOULD have` per Scala Runner specification

Debug port (5005 by default)

### `--debug-mode`

`SHOULD have` per Scala Runner specification

Debug mode (attach by default)

## Dependency options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--dependency`

Aliases: `--dep`

`MUST have` per Scala Runner specification

Add dependencies

### `--repository`

Aliases: `-r`, `--repo`

`SHOULD have` per Scala Runner specification

Add repositories

### `--compiler-plugin`

Aliases: `-P`, `--plugin`

`MUST have` per Scala Runner specification

Add compiler plugin dependencies

## Directories options

Available in commands:

[`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`install completions` , `install-completions`](./commands.md#install-completions), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--home-directory`

Aliases: `--home`

`IMPLEMENTATION specific` per Scala Runner specification

Override the path to user's home directory

## Doc options

Available in commands:

[`doc`](./commands.md#doc)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--output`

Aliases: `-o`

`MUST have` per Scala Runner specification

Set the destination path

### `--force`

Aliases: `-f`

`MUST have` per Scala Runner specification

Overwrite the destination directory, if it exists

### `--default-scaladoc-options`

Aliases: `--default-scaladoc-opts`

`SHOULD have` per Scala Runner specification

Control if Scala CLI should use default options for scaladoc, true by default. Use `--default-scaladoc-opts:false` to not include default options.

## Fmt options

Available in commands:

[`fmt` , `format` , `scalafmt`](./commands.md#fmt)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--check`

`SHOULD have` per Scala Runner specification

Check if sources are well formatted

### `--respect-project-filters`

`IMPLEMENTATION specific` per Scala Runner specification

Use project filters defined in the configuration. Turned on by default, use `--respect-project-filters:false` to disable it.

### `--save-scalafmt-conf`

`IMPLEMENTATION specific` per Scala Runner specification

Saves .scalafmt.conf file if it was created or overwritten

### `--os-arch-suffix`

`IMPLEMENTATION specific` per Scala Runner specification

### `--scalafmt-tag`

`IMPLEMENTATION specific` per Scala Runner specification

### `--scalafmt-github-org-name`

`IMPLEMENTATION specific` per Scala Runner specification

### `--scalafmt-extension`

`IMPLEMENTATION specific` per Scala Runner specification

### `--scalafmt-launcher`

`IMPLEMENTATION specific` per Scala Runner specification

### `--scalafmt-arg`

Aliases: `-F`

`IMPLEMENTATION specific` per Scala Runner specification

Pass argument to scalafmt.

### `--scalafmt-conf`

Aliases: `--scalafmt-config`

`IMPLEMENTATION specific` per Scala Runner specification

Custom path to the scalafmt configuration file.

### `--scalafmt-conf-str`

Aliases: `--scalafmt-conf-snippet`, `--scalafmt-config-str`

`IMPLEMENTATION specific` per Scala Runner specification

Pass configuration as a string.

### `--scalafmt-dialect`

Aliases: `--dialect`

`IMPLEMENTATION specific` per Scala Runner specification

Pass a global dialect for scalafmt. This overrides whatever value is configured in the .scalafmt.conf file or inferred based on Scala version used.

### `--scalafmt-version`

Aliases: `--fmt-version`

`IMPLEMENTATION specific` per Scala Runner specification

Pass scalafmt version before running it (3.5.9 by default). If passed, this overrides whatever value is configured in the .scalafmt.conf file.

## Help options

Available in commands:

[`about`](./commands.md#about), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`doctor`](./commands.md#doctor), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`help`](./commands.md#help), [`install completions` , `install-completions`](./commands.md#install-completions), [`install-home`](./commands.md#install-home), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions), [`update`](./commands.md#update), [`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--usage`

`IMPLEMENTATION specific` per Scala Runner specification

Print usage and exit

### `--help`

Aliases: `-h`, `-help`

`IMPLEMENTATION specific` per Scala Runner specification

Print help message and exit

### `--help-full`

Aliases: `--full-help`, `-full-help`, `-help-full`

`IMPLEMENTATION specific` per Scala Runner specification

Print help message, including hidden options, and exit

## Help group options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--help-js`

`IMPLEMENTATION specific` per Scala Runner specification

Show options for ScalaJS

### `--help-native`

`IMPLEMENTATION specific` per Scala Runner specification

Show options for ScalaNative

### `--help-scaladoc`

Aliases: `--doc-help`, `--help-doc`, `--scaladoc-help`

`IMPLEMENTATION specific` per Scala Runner specification

Show options for Scaladoc

### `--help-repl`

Aliases: `--repl-help`

`IMPLEMENTATION specific` per Scala Runner specification

Show options for Scala REPL

### `--help-scalafmt`

Aliases: `--fmt-help`, `--help-fmt`, `--scalafmt-help`

`IMPLEMENTATION specific` per Scala Runner specification

Show options for Scalafmt

## Install completions options

Available in commands:

[`install completions` , `install-completions`](./commands.md#install-completions)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--format`

Aliases: `--shell`

`IMPLEMENTATION specific` per Scala Runner specification

Name of the shell, either zsh or bash

### `--rc-file`

`IMPLEMENTATION specific` per Scala Runner specification

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

### `--output`

Aliases: `-o`

`IMPLEMENTATION specific` per Scala Runner specification

Completions output directory

### `--banner`

`IMPLEMENTATION specific` per Scala Runner specification

Custom banner in comment placed in rc file

### `--name`

`IMPLEMENTATION specific` per Scala Runner specification

Custom completions name

### `--env`

`IMPLEMENTATION specific` per Scala Runner specification

Print completions to stdout

## Java options

Available in commands:

[`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--java-opt`

Aliases: `-J`

`MUST have` per Scala Runner specification

Set Java options, such as `-Xmx1g`

### `--java-prop`

`MUST have` per Scala Runner specification

Set Java properties

## Jvm options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--java-home`

`SHOULD have` per Scala Runner specification

Set the Java home directory

### `--jvm`

Aliases: `-j`

`SHOULD have` per Scala Runner specification

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

### `--jvm-index`

`IMPLEMENTATION specific` per Scala Runner specification

JVM index URL

### `--jvm-index-os`

`IMPLEMENTATION specific` per Scala Runner specification

Operating system to use when looking up in the JVM index

### `--jvm-index-arch`

`IMPLEMENTATION specific` per Scala Runner specification

CPU architecture to use when looking up in the JVM index

### `--javac-plugin`

`SHOULD have` per Scala Runner specification

Javac plugin dependencies or files

### `--javac-option`

Aliases: `--javac-opt`

`SHOULD have` per Scala Runner specification

Javac options

### `--bsp-debug-port`

`IMPLEMENTATION specific` per Scala Runner specification

Port for BSP debugging

## Logging options

Available in commands:

[`about`](./commands.md#about), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`doctor`](./commands.md#doctor), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`help`](./commands.md#help), [`install completions` , `install-completions`](./commands.md#install-completions), [`install-home`](./commands.md#install-home), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions), [`update`](./commands.md#update), [`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--quiet`

Aliases: `-q`

`IMPLEMENTATION specific` per Scala Runner specification

Decrease verbosity

### `--progress`

`IMPLEMENTATION specific` per Scala Runner specification

Use progress bars

## Main class options

Available in commands:

[`run`](./commands.md#run), [`shebang`](./commands.md#shebang)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--main-class`

Aliases: `-M`

`MUST have` per Scala Runner specification

Specify which main class to run

### `--main-class-ls`

Aliases: `--list-main-class`, `--list-main-classes`, `--main-class-list`

`SHOULD have` per Scala Runner specification

List main classes available in the current context

## Run options

Available in commands:

[`run`](./commands.md#run), [`shebang`](./commands.md#shebang)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--command`

`SHOULD have` per Scala Runner specification

Print the command that would have been run (one argument per line), rather than running it

### `--scratch-dir`

`IMPLEMENTATION specific` per Scala Runner specification

Temporary / working directory where to write generated launchers

### `--use-manifest`

`IMPLEMENTATION specific` per Scala Runner specification

Run Java commands using a manifest-based class path (shortens command length)

## Scala.js options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--js`

`SHOULD have` per Scala Runner specification

Enable Scala.js. To show more options for Scala.js pass `--help-js`

### `--js-version`

`SHOULD have` per Scala Runner specification

The Scala.js version (1.11.0 by default).

### `--js-mode`

`SHOULD have` per Scala Runner specification

The Scala.js mode, either `dev` or `release`

### `--js-module-kind`

`SHOULD have` per Scala Runner specification

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

### `--js-check-ir`

`SHOULD have` per Scala Runner specification

### `--js-emit-source-maps`

`SHOULD have` per Scala Runner specification

Emit source maps

### `--js-source-maps-path`

`SHOULD have` per Scala Runner specification

Set the destination path of source maps

### `--js-dom`

`SHOULD have` per Scala Runner specification

Enable jsdom

### `--js-header`

`SHOULD have` per Scala Runner specification

A header that will be added at the top of generated .js files

### `--js-allow-big-ints-for-longs`

`IMPLEMENTATION specific` per Scala Runner specification

Primitive Longs *may* be compiled as primitive JavaScript bigints

### `--js-avoid-classes`

`IMPLEMENTATION specific` per Scala Runner specification

Avoid class'es when using functions and prototypes has the same observable semantics.

### `--js-avoid-lets-and-consts`

`IMPLEMENTATION specific` per Scala Runner specification

Avoid lets and consts when using vars has the same observable semantics.

### `--js-module-split-style`

`IMPLEMENTATION specific` per Scala Runner specification

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

### `--js-small-module-for-package`

`IMPLEMENTATION specific` per Scala Runner specification

Create as many small modules as possible for the classes in the passed packages and their subpackages.

### `--js-es-version`

`SHOULD have` per Scala Runner specification

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

### `--js-linker-path`

`IMPLEMENTATION specific` per Scala Runner specification

Path to the Scala.js linker

### `--js-cli-version`

`IMPLEMENTATION specific` per Scala Runner specification

Scala.js CLI version to use for linking (1.1.2-sc1 by default).

### `--js-cli-java-arg`

`IMPLEMENTATION specific` per Scala Runner specification

Scala.js CLI Java options

### `--js-cli-on-jvm`

`IMPLEMENTATION specific` per Scala Runner specification

Whether to run the Scala.js CLI on the JVM or using a native executable

## Scala Native options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--native`

`SHOULD have` per Scala Runner specification

Enable Scala Native. To show more options for Scala Native pass `--help-native`

### `--native-version`

`SHOULD have` per Scala Runner specification

Set the Scala Native version (0.4.8 by default).

### `--native-mode`

`SHOULD have` per Scala Runner specification

Set Scala Native compilation mode

### `--native-gc`

`SHOULD have` per Scala Runner specification

Set the Scala Native garbage collector

### `--native-clang`

`IMPLEMENTATION specific` per Scala Runner specification

Path to the Clang command

### `--native-clangpp`

`IMPLEMENTATION specific` per Scala Runner specification

Path to the Clang++ command

### `--native-linking`

`SHOULD have` per Scala Runner specification

Extra options passed to `clang` verbatim during linking

### `--native-linking-defaults`

`IMPLEMENTATION specific` per Scala Runner specification

Use default linking settings

### `--native-compile`

`SHOULD have` per Scala Runner specification

List of compile options

### `--native-compile-defaults`

`IMPLEMENTATION specific` per Scala Runner specification

Use default compile options

### `--embed-resources`

`SHOULD have` per Scala Runner specification

Embed resources into the Scala Native binary (can be read with the Java resources API)

## Scalac options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scalac-option`

Aliases: `-O`, `--scala-opt`, `--scala-option`

`IMPLEMENTATION specific` per Scala Runner specification

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

## Scalac extra options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scalac-help`

Aliases: `--help-scalac`

`IMPLEMENTATION specific` per Scala Runner specification

Show help for scalac. This is an alias for --scalac-option -help

### `--scalac-verbose`

Aliases: `--verbose-scalac`

`IMPLEMENTATION specific` per Scala Runner specification

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

## Shared options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scala-version`

Aliases: `-S`, `--scala`

`MUST have` per Scala Runner specification

Set the Scala version (3.2.1 by default)

### `--scala-binary-version`

Aliases: `-B`, `--scala-bin`, `--scala-binary`

`MUST have` per Scala Runner specification

Set the Scala binary version

### `--extra-jars`

Aliases: `--class`, `--class-path`, `--classes`, `--classpath`, `-classpath`, `-cp`, `--extra-class`, `--extra-class-path`, `--extra-classes`, `--extra-jar`, `--jar`, `--jars`

`MUST have` per Scala Runner specification

Add extra JARs and compiled classes to the class path

### `--extra-compile-only-jars`

Aliases: `--compile-only-jar`, `--compile-only-jars`, `--extra-compile-only-jar`

`SHOULD have` per Scala Runner specification

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

### `--extra-source-jars`

Aliases: `--extra-source-jar`, `--source-jar`, `--source-jars`

`SHOULD have` per Scala Runner specification

Add extra source JARs

### `--resource-dirs`

Aliases: `--resource-dir`

`MUST have` per Scala Runner specification

Add a resource directory

### `--platform`

`SHOULD have` per Scala Runner specification

Specify platform

### `--scala-library`

`IMPLEMENTATION specific` per Scala Runner specification

### `--java`

`IMPLEMENTATION specific` per Scala Runner specification

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

### `--runner`

`IMPLEMENTATION specific` per Scala Runner specification

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

### `--semantic-db`

`SHOULD have` per Scala Runner specification

Generate SemanticDBs

### `--add-stubs`

`IMPLEMENTATION specific` per Scala Runner specification

Add dependency for stubs needed to make $ivy and $dep imports to work.

### `--strict-bloop-json-check`

`IMPLEMENTATION specific` per Scala Runner specification

### `--compilation-output`

Aliases: `--compile-out`, `--compile-output`, `-d`, `--destination`, `--output-directory`

`MUST have` per Scala Runner specification

Copy compilation results to output directory using either relative or absolute path

## Snippet options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--script-snippet`

`SHOULD have` per Scala Runner specification

Allows to execute a passed string as a Scala script

### `--execute-script`

Aliases: `-e`, `--execute-sc`, `--execute-scala-script`

`SHOULD have` per Scala Runner specification

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

### `--scala-snippet`

`SHOULD have` per Scala Runner specification

Allows to execute a passed string as Scala code

### `--execute-scala`

`IMPLEMENTATION specific` per Scala Runner specification

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

### `--java-snippet`

`IMPLEMENTATION specific` per Scala Runner specification

Allows to execute a passed string as Java code

### `--execute-java`

`IMPLEMENTATION specific` per Scala Runner specification

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

### `--markdown-snippet`

Aliases: `--md-snippet`

`IMPLEMENTATION specific` per Scala Runner specification

[experimental] Allows to execute a passed string as Markdown code

### `--execute-markdown`

Aliases: `--execute-md`

`IMPLEMENTATION specific` per Scala Runner specification

[experimental] A synonym to --markdown-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

## Test options

Available in commands:

[`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--test-framework`

`SHOULD have` per Scala Runner specification

Name of the test framework's runner class to use while running tests

### `--require-tests`

`SHOULD have` per Scala Runner specification

Fail if no test suites were run

### `--test-only`

`SHOULD have` per Scala Runner specification

Specify a glob pattern to filter the tests suite to be run.

## Uninstall options

Available in commands:

[`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--force`

Aliases: `-f`

`IMPLEMENTATION specific` per Scala Runner specification

Force scala-cli uninstall

### `--skip-cache`

`IMPLEMENTATION specific` per Scala Runner specification

Don't clear scala-cli cache

### `--binary-name`

`IMPLEMENTATION specific` per Scala Runner specification

Binary name

### `--bin-dir`

`IMPLEMENTATION specific` per Scala Runner specification

Binary directory

## Uninstall completions options

Available in commands:

[`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--rc-file`

`IMPLEMENTATION specific` per Scala Runner specification

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

### `--banner`

`IMPLEMENTATION specific` per Scala Runner specification

Custom banner in comment placed in rc file

### `--name`

`IMPLEMENTATION specific` per Scala Runner specification

Custom completions name

## Update options

Available in commands:

[`update`](./commands.md#update)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--binary-name`

`IMPLEMENTATION specific` per Scala Runner specification

Binary name

### `--bin-dir`

`IMPLEMENTATION specific` per Scala Runner specification

Binary directory

### `--force`

Aliases: `-f`

`IMPLEMENTATION specific` per Scala Runner specification

Force update scala-cli if is outdated

### `--is-internal-run`

`IMPLEMENTATION specific` per Scala Runner specification

### `--gh-token`

`IMPLEMENTATION specific` per Scala Runner specification

A github token used to access GitHub. Not needed in most cases.

## Verbosity options

Available in commands:

[`about`](./commands.md#about), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`doctor`](./commands.md#doctor), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`help`](./commands.md#help), [`install completions` , `install-completions`](./commands.md#install-completions), [`install-home`](./commands.md#install-home), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions), [`update`](./commands.md#update), [`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--verbose`

Aliases: `-v`

`IMPLEMENTATION specific` per Scala Runner specification

Increase verbosity (can be specified multiple times)

### `--interactive`

Aliases: `-i`

`IMPLEMENTATION specific` per Scala Runner specification

Interactive mode

### `--actions`

`IMPLEMENTATION specific` per Scala Runner specification

Enable actionable diagnostics

## Version options

Available in commands:

[`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--cli-version`

Aliases: `--cli`

`IMPLEMENTATION specific` per Scala Runner specification

Show only plain version

### `--scala-version`

Aliases: `--scala`

`IMPLEMENTATION specific` per Scala Runner specification

Show only plain scala version

## Watch options

Available in commands:

[`compile`](./commands.md#compile), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--watch`

Aliases: `-w`

`SHOULD have` per Scala Runner specification

Watch source files for changes

### `--restart`

Aliases: `--revolver`

`SHOULD have` per Scala Runner specification

Run your application in background and automatically restart if sources have been changed

## Internal options 
### About options

Available in commands:

[`about`](./commands.md#about)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--gh-token`

`IMPLEMENTATION specific` per Scala Runner specification

A github token used to access GitHub. Not needed in most cases.

### Bsp options

Available in commands:

[`bsp`](./commands.md#bsp)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--json-options`

`IMPLEMENTATION specific` per Scala Runner specification

Command-line options JSON file

### Bsp file options

Available in commands:

[`clean`](./commands.md#clean), [`setup-ide`](./commands.md#setup-ide)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--bsp-directory`

Aliases: `--bsp-dir`

`IMPLEMENTATION specific` per Scala Runner specification

Custom BSP configuration location

### `--bsp-name`

Aliases: `--name`

`IMPLEMENTATION specific` per Scala Runner specification

Name of BSP

### Coursier options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--ttl`

`IMPLEMENTATION specific` per Scala Runner specification

Specify a TTL for changing dependencies, such as snapshots

### `--cache`

`IMPLEMENTATION specific` per Scala Runner specification

Set the coursier cache location

### `--coursier-validate-checksums`

`IMPLEMENTATION specific` per Scala Runner specification

Enable checksum validation of artifacts downloaded by coursier

### Doctor options

Available in commands:

[`doctor`](./commands.md#doctor)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--gh-token`

`IMPLEMENTATION specific` per Scala Runner specification

A github token used to access GitHub. Not needed in most cases.

### Input options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--default-forbidden-directories`

`IMPLEMENTATION specific` per Scala Runner specification

### `--forbid`

`IMPLEMENTATION specific` per Scala Runner specification

### Install home options

Available in commands:

[`install-home`](./commands.md#install-home)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scala-cli-binary-path`

`IMPLEMENTATION specific` per Scala Runner specification

### `--force`

Aliases: `-f`

`IMPLEMENTATION specific` per Scala Runner specification

Overwrite if it exists

### `--binary-name`

`IMPLEMENTATION specific` per Scala Runner specification

Binary name

### `--env`

`IMPLEMENTATION specific` per Scala Runner specification

Print the update to `env` variable

### `--bin-dir`

`IMPLEMENTATION specific` per Scala Runner specification

Binary directory

### Repl options

Available in commands:

[`repl` , `console`](./commands.md#repl)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--repl-dry-run`

`IMPLEMENTATION specific` per Scala Runner specification

Don't actually run the REPL, just fetch it

### Setup IDE options

Available in commands:

[`setup-ide`](./commands.md#setup-ide)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--charset`

`IMPLEMENTATION specific` per Scala Runner specification

### Workspace options

Available in commands:

[`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--workspace`

`IMPLEMENTATION specific` per Scala Runner specification

Directory where .scala-build is written

