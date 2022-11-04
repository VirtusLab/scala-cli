---
title: Command-line options
sidebar_position: 1
---

**This document describes as scala-cli behaves if run as `scala` command. See more information in [SIP-46](https://github.com/scala/improvement-proposals/pull/46)**

This is a summary of options that are available for each subcommand of the `scala-cli` command.

## Compilation server options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--bloop-bsp-protocol`

[Internal]
Protocol to use to open a BSP connection with Bloop

### `--bloop-bsp-socket`

[Internal]
Socket file to use to open a BSP connection with Bloop

### `--bloop-host`

[Internal]
Host the compilation server should bind to

### `--bloop-port`

[Internal]
Port the compilation server should bind to (pass `-1` to pick a random port)

### `--bloop-daemon-dir`

[Internal]
Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

### `--bloop-version`

[Internal]
If Bloop isn't already running, the version we should start

### `--bloop-bsp-timeout`

[Internal]
Maximum duration to wait for the BSP connection to be opened

### `--bloop-bsp-check-period`

[Internal]
Duration between checks of the BSP connection state

### `--bloop-startup-timeout`

[Internal]
Maximum duration to wait for the compilation server to start up

### `--bloop-default-java-opts`

[Internal]
Include default JVM options for Bloop

### `--bloop-java-opt`

[Internal]
Pass java options to use by Bloop server

### `--bloop-global-options-file`

[Internal]
Bloop global options file

### `--bloop-jvm`

[Internal]
JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

### `--bloop-working-dir`

[Internal]
Working directory for Bloop, if it needs to be started

### `--server`

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

## Compile options

Available in commands:

[`compile`](./commands.md#compile)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--print-class-path`

Aliases: `-p`, `--print-classpath`

Print the resulting class path

### `--test`

Compile test scope

## Debug options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--debug`

Turn debugging on

### `--debug-port`

Debug port (5005 by default)

### `--debug-mode`

Debug mode (attach by default)

## Dependency options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--dependency`

Aliases: `--dep`

Add dependencies

### `--repository`

Aliases: `--repo`, `-r`

Add repositories

### `--compiler-plugin`

Aliases: `-P`, `--plugin`

Add compiler plugin dependencies

## Dependency update options

Available in commands:

[`dependency-update`](./commands.md#dependency-update)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--all`

Update all dependencies if newer version was released

## Directories options

Available in commands:

[`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`install completions` , `install-completions`](./commands.md#install-completions), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--home-directory`

Aliases: `--home`

## Doc options

Available in commands:

[`doc`](./commands.md#doc)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--output`

Aliases: `-o`

Set the destination path

### `--force`

Aliases: `-f`

Overwrite the destination directory, if it exists

### `--default-scaladoc-options`

Aliases: `--default-scaladoc-opts`

Control if scala CLI should use default options for scaladoc, true by default. Use `--default-scaladoc-opts:false` to not include default options.

## Fmt options

Available in commands:

[`fmt` , `format` , `scalafmt`](./commands.md#fmt)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--check`

Check if sources are well formatted

### `--respect-project-filters`

Use project filters defined in the configuration. Turned on by default, use `--respect-project-filters:false` to disable it.

### `--save-scalafmt-conf`

Saves .scalafmt.conf file if it was created or overwritten

### `--os-arch-suffix`

[Internal]
### `--scalafmt-tag`

[Internal]
### `--scalafmt-github-org-name`

[Internal]
### `--scalafmt-extension`

[Internal]
### `--scalafmt-launcher`

[Internal]
### `--scalafmt-arg`

Aliases: `-F`

Pass argument to scalafmt.

### `--scalafmt-conf`

Aliases: `--scalafmt-config`

Custom path to the scalafmt configuration file.

### `--scalafmt-conf-str`

Aliases: `--scalafmt-config-str`, `--scalafmt-conf-snippet`

Pass configuration as a string.

### `--scalafmt-dialect`

Aliases: `--dialect`

Pass a global dialect for scalafmt. This overrides whatever value is configured in the .scalafmt.conf file or inferred based on Scala version used.

### `--scalafmt-version`

Aliases: `--fmt-version`

Pass scalafmt version before running it. This overrides whatever value is configured in the .scalafmt.conf file.

## Help options

Available in commands:

[`about`](./commands.md#about), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`doctor`](./commands.md#doctor), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`help`](./commands.md#help), [`install completions` , `install-completions`](./commands.md#install-completions), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions), [`update`](./commands.md#update), [`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--usage`

Print usage and exit

### `--help`

Aliases: `-h`, `-help`

Print help message and exit

### `--help-full`

Aliases: `--full-help`

Print help message, including hidden options, and exit

## Help group options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--help-js`

Show options for ScalaJS

### `--help-native`

Show options for ScalaNative

### `--help-scaladoc`

Aliases: `--scaladoc-help`, `--doc-help`, `--help-doc`

Show options for Scaladoc

### `--help-repl`

Aliases: `--repl-help`

Show options for Scala REPL

### `--help-scalafmt`

Aliases: `--scalafmt-help`, `--fmt-help`, `--help-fmt`

Show options for Scalafmt

## Install completions options

Available in commands:

[`install completions` , `install-completions`](./commands.md#install-completions)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--format`

Aliases: `--shell`

Name of the shell, either zsh or bash

### `--rc-file`

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

### `--output`

Aliases: `-o`

Completions output directory

### `--banner`

[Internal]
Custom banner in comment placed in rc file

### `--name`

[Internal]
Custom completions name

### `--env`

Print completions to stdout

## Java options

Available in commands:

[`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--java-opt`

Aliases: `-J`

Set Java options, such as `-Xmx1g`

### `--java-prop`

Set Java properties

## Jvm options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--java-home`

Set the Java home directory

### `--jvm`

Aliases: `-j`

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

### `--jvm-index`

[Internal]
JVM index URL

### `--jvm-index-os`

[Internal]
Operating system to use when looking up in the JVM index

### `--jvm-index-arch`

[Internal]
CPU architecture to use when looking up in the JVM index

### `--javac-plugin`

[Internal]
Javac plugin dependencies or files

### `--javac-option`

Aliases: `--javac-opt`

[Internal]
Javac options

### `--bsp-debug-port`

[Internal]
Port for BSP debugging

## Logging options

Available in commands:

[`about`](./commands.md#about), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`doctor`](./commands.md#doctor), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`help`](./commands.md#help), [`install completions` , `install-completions`](./commands.md#install-completions), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions), [`update`](./commands.md#update), [`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--quiet`

Aliases: `-q`

Decrease verbosity

### `--progress`

Use progress bars

## Main class options

Available in commands:

[`run`](./commands.md#run), [`shebang`](./commands.md#shebang)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--main-class`

Aliases: `-M`

Specify which main class to run

### `--main-class-ls`

Aliases: `--main-class-list`, `--list-main-class`, `--list-main-classes`

List main classes available in the current context

## Run options

Available in commands:

[`run`](./commands.md#run), [`shebang`](./commands.md#shebang)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--command`

Print the command that would have been run (one argument per line), rather than running it

### `--scratch-dir`

Temporary / working directory where to write generated launchers

### `--use-manifest`

[Internal]
Run Java commands using a manifest-based class path (shortens command length)

## Scala.js options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--js`

Enable Scala.js. To show more options for Scala.js pass `--help-js`

### `--js-version`

The Scala.js version

### `--js-mode`

The Scala.js mode, either `dev` or `release`

### `--js-module-kind`

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

### `--js-check-ir`

### `--js-emit-source-maps`

Emit source maps

### `--js-source-maps-path`

Set the destination path of source maps

### `--js-dom`

Enable jsdom

### `--js-header`

A header that will be added at the top of generated .js files

### `--js-allow-big-ints-for-longs`

Primitive Longs *may* be compiled as primitive JavaScript bigints

### `--js-avoid-classes`

Avoid class'es when using functions and prototypes has the same observable semantics.

### `--js-avoid-lets-and-consts`

Avoid lets and consts when using vars has the same observable semantics.

### `--js-module-split-style`

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

### `--js-small-module-for-package`

Create as many small modules as possible for the classes in the passed packages and their subpackages.

### `--js-es-version`

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

### `--js-linker-path`

[Internal]
Path to the Scala.js linker

### `--js-cli-version`

[Internal]
Scala.js CLI version to use for linking

### `--js-cli-java-arg`

[Internal]
Scala.js CLI Java options

### `--js-cli-on-jvm`

[Internal]
Whether to run the Scala.js CLI on the JVM or using a native executable

## Scala Native options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--native`

Enable Scala Native. To show more options for Scala Native pass `--help-native`

### `--native-version`

Set the Scala Native version

### `--native-mode`

Set Scala Native compilation mode

### `--native-gc`

Set the Scala Native garbage collector

### `--native-clang`

Path to the Clang command

### `--native-clangpp`

Path to the Clang++ command

### `--native-linking`

Extra options passed to `clang` verbatim during linking

### `--native-linking-defaults`

[Internal]
Use default linking settings

### `--native-compile`

List of compile options

### `--native-compile-defaults`

[Internal]
Use default compile options

### `--embed-resources`

Embed resources into the Scala Native binary (can be read with the Java resources API)

## Scalac options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scalac-option`

Aliases: `--scala-opt`, `-O`, `--scala-option`

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

## Scalac extra options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scalac-help`

Aliases: `--help-scalac`

Show help for scalac. This is an alias for --scalac-option -help

### `--scalac-verbose`

Aliases: `--verbose-scalac`

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

## Shared options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--scala-version`

Aliases: `--scala`, `-S`

Set the Scala version

### `--scala-binary-version`

Aliases: `--scala-binary`, `--scala-bin`, `-B`

[Internal]
Set the Scala binary version

### `--extra-jars`

Aliases: `--jar`, `--jars`, `--extra-jar`, `--class`, `--extra-class`, `--classes`, `--extra-classes`, `-classpath`, `-cp`, `--classpath`, `--class-path`, `--extra-class-path`

Add extra JARs and compiled classes to the class path

### `--extra-compile-only-jars`

Aliases: `--compile-only-jar`, `--compile-only-jars`, `--extra-compile-only-jar`

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

### `--extra-source-jars`

Aliases: `--source-jar`, `--source-jars`, `--extra-source-jar`

Add extra source JARs

### `--resource-dirs`

Aliases: `--resource-dir`

Add a resource directory

### `--platform`

Specify platform

### `--scala-library`

[Internal]
### `--java`

[Internal]
Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

### `--runner`

[Internal]
Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

### `--semantic-db`

[Internal]
Generate SemanticDBs

### `--add-stubs`

[Internal]
Add dependency for stubs needed to make $ivy and $dep imports to work.

### `--strict-bloop-json-check`

[Internal]
### `--compilation-output`

Aliases: `--output-directory`, `-d`, `--destination`, `--compile-output`, `--compile-out`

Copy compilation results to output directory using either relative or absolute path

## Snippet options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--script-snippet`

Allows to execute a passed string as a Scala script

### `--execute-script`

Aliases: `--execute-scala-script`, `--execute-sc`, `-e`

[Internal]
A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

### `--scala-snippet`

Allows to execute a passed string as Scala code

### `--execute-scala`

[Internal]
A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

### `--java-snippet`

Allows to execute a passed string as Java code

### `--execute-java`

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

## Test options

Available in commands:

[`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--test-framework`

Name of the test framework's runner class to use while running tests

### `--require-tests`

Fail if no test suites were run

## Uninstall options

Available in commands:

[`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--force`

Aliases: `-f`

Force scala-cli uninstall

### `--skip-cache`

[Internal]
Don't clear scala-cli cache

### `--binary-name`

[Internal]
Binary name

### `--bin-dir`

[Internal]
Binary directory

## Uninstall completions options

Available in commands:

[`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--rc-file`

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

### `--banner`

[Internal]
Custom banner in comment placed in rc file

### `--name`

[Internal]
Custom completions name

## Update options

Available in commands:

[`update`](./commands.md#update)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--binary-name`

[Internal]
Binary name

### `--bin-dir`

[Internal]
Binary directory

### `--force`

Aliases: `-f`

Force update scala-cli if is outdated

### `--is-internal-run`

[Internal]
### `--gh-token`

[Internal]
A github token used to access GitHub. Not needed in most cases.

## Verbosity options

Available in commands:

[`about`](./commands.md#about), [`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`doctor`](./commands.md#doctor), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`help`](./commands.md#help), [`install completions` , `install-completions`](./commands.md#install-completions), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall), [`uninstall completions` , `uninstall-completions`](./commands.md#uninstall-completions), [`update`](./commands.md#update), [`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--verbose`

Aliases: `-v`

Increase verbosity (can be specified multiple times)

### `--interactive`

Aliases: `-i`

Interactive mode

### `--actions`

Enable actionable diagnostics

## Version options

Available in commands:

[`version`](./commands.md#version)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--cli-version`

Aliases: `--cli`

Show only plain scala-cli version

### `--scala-version`

Aliases: `--scala`

Show only plain scala version

## Watch options

Available in commands:

[`compile`](./commands.md#compile), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--watch`

Aliases: `-w`

Watch source files for changes

### `--restart`

Aliases: `--revolver`

Run your application in background and automatically restart if sources have been changed

## Internal options 
### About options

Available in commands:

[`about`](./commands.md#about)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--gh-token`

[Internal]
A github token used to access GitHub. Not needed in most cases.

### Bsp options

Available in commands:

[`bsp`](./commands.md#bsp)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--json-options`

[Internal]
Command-line options JSON file

### Bsp file options

Available in commands:

[`clean`](./commands.md#clean), [`setup-ide`](./commands.md#setup-ide)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--bsp-directory`

Aliases: `--bsp-dir`

[Internal]
Custom BSP configuration location

### `--bsp-name`

Aliases: `--name`

[Internal]
Name of BSP

### Coursier options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test), [`uninstall`](./commands.md#uninstall)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--ttl`

[Internal]
Specify a TTL for changing dependencies, such as snapshots

### `--cache`

[Internal]
Set the coursier cache location

### `--coursier-validate-checksums`

[Internal]
Enable checksum validation of artifacts downloaded by coursier

### Doctor options

Available in commands:

[`doctor`](./commands.md#doctor)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--gh-token`

[Internal]
A github token used to access GitHub. Not needed in most cases.

### Input options

Available in commands:

[`bsp`](./commands.md#bsp), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--default-forbidden-directories`

[Internal]
### `--forbid`

[Internal]
### Repl options

Available in commands:

[`repl` , `console`](./commands.md#repl)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--repl-dry-run`

[Internal]
Don't actually run the REPL, just fetch it

### Setup IDE options

Available in commands:

[`setup-ide`](./commands.md#setup-ide)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--charset`

[Internal]
### Workspace options

Available in commands:

[`bsp`](./commands.md#bsp), [`clean`](./commands.md#clean), [`compile`](./commands.md#compile), [`dependency-update`](./commands.md#dependency-update), [`doc`](./commands.md#doc), [`fmt` , `format` , `scalafmt`](./commands.md#fmt), [`repl` , `console`](./commands.md#repl), [`run`](./commands.md#run), [`setup-ide`](./commands.md#setup-ide), [`shebang`](./commands.md#shebang), [`test`](./commands.md#test)

<!-- Automatically generated, DO NOT EDIT MANUALLY -->

### `--workspace`

[Internal]
Directory where .scala-build is written

