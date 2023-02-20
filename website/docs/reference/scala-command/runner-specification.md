---
title: Scala Runner specification
sidebar_position: 1
---
      

**This document describes proposed specification for Scala runner based on Scala CLI documentation as requested per [SIP-46](https://github.com/scala/improvement-proposals/pull/46)**

Commands and options are marked with MUST and SHOULD (in the RFC style) for ones applicable for Scala Runner.
Options and commands marked as **Implementation** are needed for smooth running of Scala CLI.
We recommend for those options and commands to be supported by the `scala` command (when based on Scala CLI) but not to be a part of the Scala Runner specification.

The proposed Scala runner specification should also contain supported `Using directives` defined in the dedicated [document](./directives.md)]

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



# MUST have commands

## `compile` command
**MUST have for Scala Runner specification.**

Compile Scala code.

Specific compile configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/compile

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `--plugin` ,`-P`

**--scala-version**

Set the Scala version (3.2.2 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-bin` ,`--scala-binary`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--extra-class-path` ,`--class-path` ,`--classpath` ,`-cp` ,`-classpath` ,`--extra-classes` ,`--classes` ,`--extra-class` ,`--class` ,`--extra-jar` ,`--jars` ,`--jar`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `-with-compiler` ,`--with-scala-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--compile-out` ,`--compile-output` ,`--destination` ,`-d` ,`--output-directory`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.12.0 by default).

**--js-mode**

The Scala.js mode, either `dev` or `release`

**--js-module-kind**

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**--js-check-ir**



**--js-emit-source-maps**

Emit source maps

**--js-source-maps-path**

Set the destination path of source maps

**--js-dom**

Enable jsdom

**--js-header**

A header that will be added at the top of generated .js files

**--js-es-version**

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

**--native**

Enable Scala Native. To show more options for Scala Native pass `--help-native`

**--native-version**

Set the Scala Native version (0.4.9 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories

Aliases: `-r` ,`--repo`

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

**--script-snippet**

Allows to execute a passed string as a Scala script

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `-e` ,`--execute-sc` ,`--execute-scala-script`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--extra-compile-only-jar` ,`--compile-only-jars` ,`--compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--extra-source-jar` ,`--source-jars` ,`--source-jar`

**--platform**

Specify platform

**--semantic-db**

Generate SemanticDBs

**--watch**

Run the application in the background, automatically wake the thread and re-run if sources have been changed

Aliases: `-w`

**--restart**

Run the application in the background, automatically kill the process and restart if sources have been changed

Aliases: `--revolver`

**--print-class-path**

Print the resulting class path

Aliases: `--print-classpath` ,`-p`

**--test**

Compile test scope

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--suppress-directives-in-multiple-files-warning**

Suppress warnings about using directives in multiple files

Aliases: `--suppress-warning-directives-in-multiple-files`

**--suppress-outdated-dependency-warning**

Suppress warnings about outdated dependencies in project

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--js-allow-big-ints-for-longs**

Primitive Longs *may* be compiled as primitive JavaScript bigints

**--js-avoid-classes**

Avoid class'es when using functions and prototypes has the same observable semantics.

**--js-avoid-lets-and-consts**

Avoid lets and consts when using vars has the same observable semantics.

**--js-module-split-style**

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

**--js-small-module-for-package**

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**--js-linker-path**

Path to the Scala.js linker

**--js-cli-version**

Scala.js CLI version to use for linking (1.1.3-sc1 by default).

**--js-cli-java-arg**

Scala.js CLI Java options

**--js-cli-on-jvm**

Whether to run the Scala.js CLI on the JVM or using a native executable

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--native-linking-defaults**

Use default linking settings

**--native-compile-defaults**

Use default compile options

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--workspace**

Directory where .scala-build is written

**--scalac-help**

Show help for scalac. This is an alias for --scalac-option -help

Aliases: `--help-scalac`

**--scalac-verbose**

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

Aliases: `--verbose-scalac`

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--scala-library**



**--java**

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

**--runner**

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

**--add-stubs**

Add dependency for stubs needed to make $ivy and $dep imports to work.

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--doc-help` ,`--scaladoc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--fmt-help` ,`--scalafmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

</details>

---

## `config` command
**MUST have for Scala Runner specification.**

Configure global settings for Scala CLI.

Syntax:
  scala-cli config key value
For example, to globally set the interactive mode:
  scala-cli config interactive true

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/misc/config

### SHOULD have options

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--signing-cli-version**



**--signing-cli-java-arg**



**--force-jvm-signing-cli**

Whether to run the Scala Signing CLI on the JVM or using a native executable

**--dump**

Dump config DB as JSON

**--create-pgp-key**

Create PGP key in config

**--unset**

Remove an entry from config

Aliases: `--remove`

</details>

---

## `doc` command
**MUST have for Scala Runner specification.**

Generate Scaladoc documentation.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/doc

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `--plugin` ,`-P`

**--scala-version**

Set the Scala version (3.2.2 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-bin` ,`--scala-binary`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--extra-class-path` ,`--class-path` ,`--classpath` ,`-cp` ,`-classpath` ,`--extra-classes` ,`--classes` ,`--extra-class` ,`--class` ,`--extra-jar` ,`--jars` ,`--jar`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `-with-compiler` ,`--with-scala-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--compile-out` ,`--compile-output` ,`--destination` ,`-d` ,`--output-directory`

**--output**

Set the destination path

Aliases: `-o`

**--force**

Overwrite the destination directory, if it exists

Aliases: `-f`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.12.0 by default).

**--js-mode**

The Scala.js mode, either `dev` or `release`

**--js-module-kind**

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**--js-check-ir**



**--js-emit-source-maps**

Emit source maps

**--js-source-maps-path**

Set the destination path of source maps

**--js-dom**

Enable jsdom

**--js-header**

A header that will be added at the top of generated .js files

**--js-es-version**

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

**--native**

Enable Scala Native. To show more options for Scala Native pass `--help-native`

**--native-version**

Set the Scala Native version (0.4.9 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories

Aliases: `-r` ,`--repo`

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

**--script-snippet**

Allows to execute a passed string as a Scala script

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `-e` ,`--execute-sc` ,`--execute-scala-script`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--extra-compile-only-jar` ,`--compile-only-jars` ,`--compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--extra-source-jar` ,`--source-jars` ,`--source-jar`

**--platform**

Specify platform

**--semantic-db**

Generate SemanticDBs

**--default-scaladoc-options**

Control if Scala CLI should use default options for scaladoc, true by default. Use `--default-scaladoc-opts:false` to not include default options.

Aliases: `--default-scaladoc-opts`

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--suppress-directives-in-multiple-files-warning**

Suppress warnings about using directives in multiple files

Aliases: `--suppress-warning-directives-in-multiple-files`

**--suppress-outdated-dependency-warning**

Suppress warnings about outdated dependencies in project

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--js-allow-big-ints-for-longs**

Primitive Longs *may* be compiled as primitive JavaScript bigints

**--js-avoid-classes**

Avoid class'es when using functions and prototypes has the same observable semantics.

**--js-avoid-lets-and-consts**

Avoid lets and consts when using vars has the same observable semantics.

**--js-module-split-style**

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

**--js-small-module-for-package**

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**--js-linker-path**

Path to the Scala.js linker

**--js-cli-version**

Scala.js CLI version to use for linking (1.1.3-sc1 by default).

**--js-cli-java-arg**

Scala.js CLI Java options

**--js-cli-on-jvm**

Whether to run the Scala.js CLI on the JVM or using a native executable

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--native-linking-defaults**

Use default linking settings

**--native-compile-defaults**

Use default compile options

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--workspace**

Directory where .scala-build is written

**--scalac-help**

Show help for scalac. This is an alias for --scalac-option -help

Aliases: `--help-scalac`

**--scalac-verbose**

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

Aliases: `--verbose-scalac`

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--scala-library**



**--java**

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

**--runner**

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

**--add-stubs**

Add dependency for stubs needed to make $ivy and $dep imports to work.

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--doc-help` ,`--scaladoc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--fmt-help` ,`--scalafmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

</details>

---

## `repl` command
**MUST have for Scala Runner specification.**

Aliases: `console`

Fire-up a Scala REPL.

The entire Scala CLI project's classpath is loaded to the repl.

Specific repl configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/repl

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `--plugin` ,`-P`

**--scala-version**

Set the Scala version (3.2.2 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-bin` ,`--scala-binary`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--extra-class-path` ,`--class-path` ,`--classpath` ,`-cp` ,`-classpath` ,`--extra-classes` ,`--classes` ,`--extra-class` ,`--class` ,`--extra-jar` ,`--jars` ,`--jar`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `-with-compiler` ,`--with-scala-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--compile-out` ,`--compile-output` ,`--destination` ,`-d` ,`--output-directory`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.12.0 by default).

**--js-mode**

The Scala.js mode, either `dev` or `release`

**--js-module-kind**

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**--js-check-ir**



**--js-emit-source-maps**

Emit source maps

**--js-source-maps-path**

Set the destination path of source maps

**--js-dom**

Enable jsdom

**--js-header**

A header that will be added at the top of generated .js files

**--js-es-version**

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

**--native**

Enable Scala Native. To show more options for Scala Native pass `--help-native`

**--native-version**

Set the Scala Native version (0.4.9 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories

Aliases: `-r` ,`--repo`

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

**--script-snippet**

Allows to execute a passed string as a Scala script

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `-e` ,`--execute-sc` ,`--execute-scala-script`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--extra-compile-only-jar` ,`--compile-only-jars` ,`--compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--extra-source-jar` ,`--source-jars` ,`--source-jar`

**--platform**

Specify platform

**--semantic-db**

Generate SemanticDBs

**--watch**

Run the application in the background, automatically wake the thread and re-run if sources have been changed

Aliases: `-w`

**--restart**

Run the application in the background, automatically kill the process and restart if sources have been changed

Aliases: `--revolver`

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--suppress-directives-in-multiple-files-warning**

Suppress warnings about using directives in multiple files

Aliases: `--suppress-warning-directives-in-multiple-files`

**--suppress-outdated-dependency-warning**

Suppress warnings about outdated dependencies in project

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--js-allow-big-ints-for-longs**

Primitive Longs *may* be compiled as primitive JavaScript bigints

**--js-avoid-classes**

Avoid class'es when using functions and prototypes has the same observable semantics.

**--js-avoid-lets-and-consts**

Avoid lets and consts when using vars has the same observable semantics.

**--js-module-split-style**

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

**--js-small-module-for-package**

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**--js-linker-path**

Path to the Scala.js linker

**--js-cli-version**

Scala.js CLI version to use for linking (1.1.3-sc1 by default).

**--js-cli-java-arg**

Scala.js CLI Java options

**--js-cli-on-jvm**

Whether to run the Scala.js CLI on the JVM or using a native executable

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--native-linking-defaults**

Use default linking settings

**--native-compile-defaults**

Use default compile options

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--workspace**

Directory where .scala-build is written

**--scalac-help**

Show help for scalac. This is an alias for --scalac-option -help

Aliases: `--help-scalac`

**--scalac-verbose**

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

Aliases: `--verbose-scalac`

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--scala-library**



**--java**

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

**--runner**

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

**--add-stubs**

Add dependency for stubs needed to make $ivy and $dep imports to work.

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--doc-help` ,`--scaladoc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--fmt-help` ,`--scalafmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--java-prop-option**

Add java properties. Note that options equal `-Dproperty=value` are assumed to be java properties and don't require to be passed after `--java-prop`.

Aliases: `--java-prop`

**--repl-dry-run**

Don't actually run the REPL, just fetch it

</details>

---

## `run` command
**MUST have for Scala Runner specification.**

Compile and run Scala code.

You are currently viewing the basic help for the run sub-command. You can view the full help by running: 
   [1mscala-cli run --help-full[0m
For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/run

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `--plugin` ,`-P`

**--scala-version**

Set the Scala version (3.2.2 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-bin` ,`--scala-binary`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--extra-class-path` ,`--class-path` ,`--classpath` ,`-cp` ,`-classpath` ,`--extra-classes` ,`--classes` ,`--extra-class` ,`--class` ,`--extra-jar` ,`--jars` ,`--jar`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `-with-compiler` ,`--with-scala-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--compile-out` ,`--compile-output` ,`--destination` ,`-d` ,`--output-directory`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

**--main-class**

Specify which main class to run

Aliases: `-M`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.12.0 by default).

**--js-mode**

The Scala.js mode, either `dev` or `release`

**--js-module-kind**

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**--js-check-ir**



**--js-emit-source-maps**

Emit source maps

**--js-source-maps-path**

Set the destination path of source maps

**--js-dom**

Enable jsdom

**--js-header**

A header that will be added at the top of generated .js files

**--js-es-version**

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

**--native**

Enable Scala Native. To show more options for Scala Native pass `--help-native`

**--native-version**

Set the Scala Native version (0.4.9 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories

Aliases: `-r` ,`--repo`

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

**--script-snippet**

Allows to execute a passed string as a Scala script

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `-e` ,`--execute-sc` ,`--execute-scala-script`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--extra-compile-only-jar` ,`--compile-only-jars` ,`--compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--extra-source-jar` ,`--source-jars` ,`--source-jar`

**--platform**

Specify platform

**--semantic-db**

Generate SemanticDBs

**--watch**

Run the application in the background, automatically wake the thread and re-run if sources have been changed

Aliases: `-w`

**--restart**

Run the application in the background, automatically kill the process and restart if sources have been changed

Aliases: `--revolver`

**--main-class-ls**

List main classes available in the current context

Aliases: `--list-main-classes` ,`--list-main-class` ,`--main-class-list`

**--command**

Print the command that would have been run (one argument per line), rather than running it

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--suppress-directives-in-multiple-files-warning**

Suppress warnings about using directives in multiple files

Aliases: `--suppress-warning-directives-in-multiple-files`

**--suppress-outdated-dependency-warning**

Suppress warnings about outdated dependencies in project

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--js-allow-big-ints-for-longs**

Primitive Longs *may* be compiled as primitive JavaScript bigints

**--js-avoid-classes**

Avoid class'es when using functions and prototypes has the same observable semantics.

**--js-avoid-lets-and-consts**

Avoid lets and consts when using vars has the same observable semantics.

**--js-module-split-style**

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

**--js-small-module-for-package**

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**--js-linker-path**

Path to the Scala.js linker

**--js-cli-version**

Scala.js CLI version to use for linking (1.1.3-sc1 by default).

**--js-cli-java-arg**

Scala.js CLI Java options

**--js-cli-on-jvm**

Whether to run the Scala.js CLI on the JVM or using a native executable

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--native-linking-defaults**

Use default linking settings

**--native-compile-defaults**

Use default compile options

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--workspace**

Directory where .scala-build is written

**--scalac-help**

Show help for scalac. This is an alias for --scalac-option -help

Aliases: `--help-scalac`

**--scalac-verbose**

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

Aliases: `--verbose-scalac`

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--scala-library**



**--java**

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

**--runner**

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

**--add-stubs**

Add dependency for stubs needed to make $ivy and $dep imports to work.

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--doc-help` ,`--scaladoc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--fmt-help` ,`--scalafmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--java-prop-option**

Add java properties. Note that options equal `-Dproperty=value` are assumed to be java properties and don't require to be passed after `--java-prop`.

Aliases: `--java-prop`

**--scratch-dir**

Temporary / working directory where to write generated launchers

**--use-manifest**

Run Java commands using a manifest-based class path (shortens command length)

</details>

---

## `shebang` command
**MUST have for Scala Runner specification.**

Like `run`, but handier for shebang scripts.

You are currently viewing the basic help for the shebang sub-command. You can view the full help by running: 
   [1mscala-cli shebang --help-full[0m
For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/shebang

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `--plugin` ,`-P`

**--scala-version**

Set the Scala version (3.2.2 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-bin` ,`--scala-binary`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--extra-class-path` ,`--class-path` ,`--classpath` ,`-cp` ,`-classpath` ,`--extra-classes` ,`--classes` ,`--extra-class` ,`--class` ,`--extra-jar` ,`--jars` ,`--jar`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `-with-compiler` ,`--with-scala-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--compile-out` ,`--compile-output` ,`--destination` ,`-d` ,`--output-directory`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

**--main-class**

Specify which main class to run

Aliases: `-M`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.12.0 by default).

**--js-mode**

The Scala.js mode, either `dev` or `release`

**--js-module-kind**

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**--js-check-ir**



**--js-emit-source-maps**

Emit source maps

**--js-source-maps-path**

Set the destination path of source maps

**--js-dom**

Enable jsdom

**--js-header**

A header that will be added at the top of generated .js files

**--js-es-version**

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

**--native**

Enable Scala Native. To show more options for Scala Native pass `--help-native`

**--native-version**

Set the Scala Native version (0.4.9 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories

Aliases: `-r` ,`--repo`

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

**--script-snippet**

Allows to execute a passed string as a Scala script

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `-e` ,`--execute-sc` ,`--execute-scala-script`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--extra-compile-only-jar` ,`--compile-only-jars` ,`--compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--extra-source-jar` ,`--source-jars` ,`--source-jar`

**--platform**

Specify platform

**--semantic-db**

Generate SemanticDBs

**--watch**

Run the application in the background, automatically wake the thread and re-run if sources have been changed

Aliases: `-w`

**--restart**

Run the application in the background, automatically kill the process and restart if sources have been changed

Aliases: `--revolver`

**--main-class-ls**

List main classes available in the current context

Aliases: `--list-main-classes` ,`--list-main-class` ,`--main-class-list`

**--command**

Print the command that would have been run (one argument per line), rather than running it

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--suppress-directives-in-multiple-files-warning**

Suppress warnings about using directives in multiple files

Aliases: `--suppress-warning-directives-in-multiple-files`

**--suppress-outdated-dependency-warning**

Suppress warnings about outdated dependencies in project

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--js-allow-big-ints-for-longs**

Primitive Longs *may* be compiled as primitive JavaScript bigints

**--js-avoid-classes**

Avoid class'es when using functions and prototypes has the same observable semantics.

**--js-avoid-lets-and-consts**

Avoid lets and consts when using vars has the same observable semantics.

**--js-module-split-style**

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

**--js-small-module-for-package**

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**--js-linker-path**

Path to the Scala.js linker

**--js-cli-version**

Scala.js CLI version to use for linking (1.1.3-sc1 by default).

**--js-cli-java-arg**

Scala.js CLI Java options

**--js-cli-on-jvm**

Whether to run the Scala.js CLI on the JVM or using a native executable

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--native-linking-defaults**

Use default linking settings

**--native-compile-defaults**

Use default compile options

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--workspace**

Directory where .scala-build is written

**--scalac-help**

Show help for scalac. This is an alias for --scalac-option -help

Aliases: `--help-scalac`

**--scalac-verbose**

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

Aliases: `--verbose-scalac`

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--scala-library**



**--java**

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

**--runner**

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

**--add-stubs**

Add dependency for stubs needed to make $ivy and $dep imports to work.

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--doc-help` ,`--scaladoc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--fmt-help` ,`--scalafmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--java-prop-option**

Add java properties. Note that options equal `-Dproperty=value` are assumed to be java properties and don't require to be passed after `--java-prop`.

Aliases: `--java-prop`

**--scratch-dir**

Temporary / working directory where to write generated launchers

**--use-manifest**

Run Java commands using a manifest-based class path (shortens command length)

</details>

---

# SHOULD have commands

## `fmt` command
**SHOULD have for Scala Runner specification.**

Aliases: `format`, `scalafmt`

Formats Scala code.

`scalafmt` is used to perform the formatting under the hood.

The `.scalafmt.conf` configuration file is optional.
Default configuration values will be assumed by Scala CLI.

All standard Scala CLI inputs are accepted, but only Scala sources will be formatted (.scala and .sc files).

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/fmt

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `--plugin` ,`-P`

**--scala-version**

Set the Scala version (3.2.2 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-bin` ,`--scala-binary`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--extra-class-path` ,`--class-path` ,`--classpath` ,`-cp` ,`-classpath` ,`--extra-classes` ,`--classes` ,`--extra-class` ,`--class` ,`--extra-jar` ,`--jars` ,`--jar`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `-with-compiler` ,`--with-scala-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--compile-out` ,`--compile-output` ,`--destination` ,`-d` ,`--output-directory`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.12.0 by default).

**--js-mode**

The Scala.js mode, either `dev` or `release`

**--js-module-kind**

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**--js-check-ir**



**--js-emit-source-maps**

Emit source maps

**--js-source-maps-path**

Set the destination path of source maps

**--js-dom**

Enable jsdom

**--js-header**

A header that will be added at the top of generated .js files

**--js-es-version**

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

**--native**

Enable Scala Native. To show more options for Scala Native pass `--help-native`

**--native-version**

Set the Scala Native version (0.4.9 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories

Aliases: `-r` ,`--repo`

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

**--script-snippet**

Allows to execute a passed string as a Scala script

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `-e` ,`--execute-sc` ,`--execute-scala-script`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--extra-compile-only-jar` ,`--compile-only-jars` ,`--compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--extra-source-jar` ,`--source-jars` ,`--source-jar`

**--platform**

Specify platform

**--semantic-db**

Generate SemanticDBs

**--check**

Check if sources are well formatted

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--suppress-directives-in-multiple-files-warning**

Suppress warnings about using directives in multiple files

Aliases: `--suppress-warning-directives-in-multiple-files`

**--suppress-outdated-dependency-warning**

Suppress warnings about outdated dependencies in project

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--js-allow-big-ints-for-longs**

Primitive Longs *may* be compiled as primitive JavaScript bigints

**--js-avoid-classes**

Avoid class'es when using functions and prototypes has the same observable semantics.

**--js-avoid-lets-and-consts**

Avoid lets and consts when using vars has the same observable semantics.

**--js-module-split-style**

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

**--js-small-module-for-package**

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**--js-linker-path**

Path to the Scala.js linker

**--js-cli-version**

Scala.js CLI version to use for linking (1.1.3-sc1 by default).

**--js-cli-java-arg**

Scala.js CLI Java options

**--js-cli-on-jvm**

Whether to run the Scala.js CLI on the JVM or using a native executable

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--native-linking-defaults**

Use default linking settings

**--native-compile-defaults**

Use default compile options

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--workspace**

Directory where .scala-build is written

**--scalac-help**

Show help for scalac. This is an alias for --scalac-option -help

Aliases: `--help-scalac`

**--scalac-verbose**

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

Aliases: `--verbose-scalac`

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--scala-library**



**--java**

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

**--runner**

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

**--add-stubs**

Add dependency for stubs needed to make $ivy and $dep imports to work.

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--doc-help` ,`--scaladoc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--fmt-help` ,`--scalafmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--respect-project-filters**

Use project filters defined in the configuration. Turned on by default, use `--respect-project-filters:false` to disable it.

**--save-scalafmt-conf**

Saves .scalafmt.conf file if it was created or overwritten

**--os-arch-suffix**



**--scalafmt-tag**



**--scalafmt-github-org-name**



**--scalafmt-extension**



**--scalafmt-launcher**



**--scalafmt-arg**

Pass argument to scalafmt.

Aliases: `-F`

**--scalafmt-conf**

Custom path to the scalafmt configuration file.

Aliases: `--scalafmt-config`

**--scalafmt-conf-str**

Pass configuration as a string.

Aliases: `--scalafmt-conf-snippet` ,`--scalafmt-config-str`

**--scalafmt-dialect**

Pass a global dialect for scalafmt. This overrides whatever value is configured in the .scalafmt.conf file or inferred based on Scala version used.

Aliases: `--dialect`

**--scalafmt-version**

Pass scalafmt version before running it (3.6.1 by default). If passed, this overrides whatever value is configured in the .scalafmt.conf file.

Aliases: `--fmt-version`

</details>

---

## `test` command
**SHOULD have for Scala Runner specification.**

Compile and test Scala code.

You are currently viewing the basic help for the test sub-command. You can view the full help by running: 
   [1mscala-cli test --help-full[0m
For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/test

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `--plugin` ,`-P`

**--scala-version**

Set the Scala version (3.2.2 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-bin` ,`--scala-binary`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--extra-class-path` ,`--class-path` ,`--classpath` ,`-cp` ,`-classpath` ,`--extra-classes` ,`--classes` ,`--extra-class` ,`--class` ,`--extra-jar` ,`--jars` ,`--jar`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `-with-compiler` ,`--with-scala-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--compile-out` ,`--compile-output` ,`--destination` ,`-d` ,`--output-directory`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.12.0 by default).

**--js-mode**

The Scala.js mode, either `dev` or `release`

**--js-module-kind**

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**--js-check-ir**



**--js-emit-source-maps**

Emit source maps

**--js-source-maps-path**

Set the destination path of source maps

**--js-dom**

Enable jsdom

**--js-header**

A header that will be added at the top of generated .js files

**--js-es-version**

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

**--native**

Enable Scala Native. To show more options for Scala Native pass `--help-native`

**--native-version**

Set the Scala Native version (0.4.9 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories

Aliases: `-r` ,`--repo`

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

**--script-snippet**

Allows to execute a passed string as a Scala script

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `-e` ,`--execute-sc` ,`--execute-scala-script`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--extra-compile-only-jar` ,`--compile-only-jars` ,`--compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--extra-source-jar` ,`--source-jars` ,`--source-jar`

**--platform**

Specify platform

**--semantic-db**

Generate SemanticDBs

**--watch**

Run the application in the background, automatically wake the thread and re-run if sources have been changed

Aliases: `-w`

**--restart**

Run the application in the background, automatically kill the process and restart if sources have been changed

Aliases: `--revolver`

**--test-framework**

Name of the test framework's runner class to use while running tests

**--require-tests**

Fail if no test suites were run

**--test-only**

Specify a glob pattern to filter the tests suite to be run.

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--suppress-directives-in-multiple-files-warning**

Suppress warnings about using directives in multiple files

Aliases: `--suppress-warning-directives-in-multiple-files`

**--suppress-outdated-dependency-warning**

Suppress warnings about outdated dependencies in project

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--js-allow-big-ints-for-longs**

Primitive Longs *may* be compiled as primitive JavaScript bigints

**--js-avoid-classes**

Avoid class'es when using functions and prototypes has the same observable semantics.

**--js-avoid-lets-and-consts**

Avoid lets and consts when using vars has the same observable semantics.

**--js-module-split-style**

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

**--js-small-module-for-package**

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**--js-linker-path**

Path to the Scala.js linker

**--js-cli-version**

Scala.js CLI version to use for linking (1.1.3-sc1 by default).

**--js-cli-java-arg**

Scala.js CLI Java options

**--js-cli-on-jvm**

Whether to run the Scala.js CLI on the JVM or using a native executable

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--native-linking-defaults**

Use default linking settings

**--native-compile-defaults**

Use default compile options

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--workspace**

Directory where .scala-build is written

**--scalac-help**

Show help for scalac. This is an alias for --scalac-option -help

Aliases: `--help-scalac`

**--scalac-verbose**

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

Aliases: `--verbose-scalac`

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--scala-library**



**--java**

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

**--runner**

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

**--add-stubs**

Add dependency for stubs needed to make $ivy and $dep imports to work.

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--doc-help` ,`--scaladoc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--fmt-help` ,`--scalafmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--java-prop-option**

Add java properties. Note that options equal `-Dproperty=value` are assumed to be java properties and don't require to be passed after `--java-prop`.

Aliases: `--java-prop`

</details>

---

## `version` command
**SHOULD have for Scala Runner specification.**

Prints the version of the Scala CLI and the default version of Scala.

You are currently viewing the basic help for the version sub-command. You can view the full help by running: 
   [1mscala-cli version --help-full[0m
For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/version

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--cli-version**

Show plain Scala CLI version only

Aliases: `--cli`

**--scala-version**

Show plain Scala version only

Aliases: `--scala`

**--gh-token**

A github token used to access GitHub. Not needed in most cases.

**--offline**

Don't check for the newest available Scala CLI version upstream

</details>

---

# IMPLEMENTATION specific commands

## `bsp` command
**IMPLEMENTATION specific for Scala Runner specification.**

Start BSP server.

BSP stands for Build Server Protocol.
For more information refer to https://build-server-protocol.github.io/

This sub-command is not designed to be used by a human.
It is normally supposed to be invoked by your IDE when a Scala CLI project is imported.

Detailed documentation can be found on our website: https://scala-cli.virtuslab.org

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `--plugin` ,`-P`

**--scala-version**

Set the Scala version (3.2.2 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-bin` ,`--scala-binary`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--extra-class-path` ,`--class-path` ,`--classpath` ,`-cp` ,`-classpath` ,`--extra-classes` ,`--classes` ,`--extra-class` ,`--class` ,`--extra-jar` ,`--jars` ,`--jar`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `-with-compiler` ,`--with-scala-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--compile-out` ,`--compile-output` ,`--destination` ,`-d` ,`--output-directory`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.12.0 by default).

**--js-mode**

The Scala.js mode, either `dev` or `release`

**--js-module-kind**

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**--js-check-ir**



**--js-emit-source-maps**

Emit source maps

**--js-source-maps-path**

Set the destination path of source maps

**--js-dom**

Enable jsdom

**--js-header**

A header that will be added at the top of generated .js files

**--js-es-version**

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

**--native**

Enable Scala Native. To show more options for Scala Native pass `--help-native`

**--native-version**

Set the Scala Native version (0.4.9 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories

Aliases: `-r` ,`--repo`

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

**--script-snippet**

Allows to execute a passed string as a Scala script

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `-e` ,`--execute-sc` ,`--execute-scala-script`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--extra-compile-only-jar` ,`--compile-only-jars` ,`--compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--extra-source-jar` ,`--source-jars` ,`--source-jar`

**--platform**

Specify platform

**--semantic-db**

Generate SemanticDBs

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--suppress-directives-in-multiple-files-warning**

Suppress warnings about using directives in multiple files

Aliases: `--suppress-warning-directives-in-multiple-files`

**--suppress-outdated-dependency-warning**

Suppress warnings about outdated dependencies in project

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--js-allow-big-ints-for-longs**

Primitive Longs *may* be compiled as primitive JavaScript bigints

**--js-avoid-classes**

Avoid class'es when using functions and prototypes has the same observable semantics.

**--js-avoid-lets-and-consts**

Avoid lets and consts when using vars has the same observable semantics.

**--js-module-split-style**

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

**--js-small-module-for-package**

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**--js-linker-path**

Path to the Scala.js linker

**--js-cli-version**

Scala.js CLI version to use for linking (1.1.3-sc1 by default).

**--js-cli-java-arg**

Scala.js CLI Java options

**--js-cli-on-jvm**

Whether to run the Scala.js CLI on the JVM or using a native executable

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--native-linking-defaults**

Use default linking settings

**--native-compile-defaults**

Use default compile options

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--workspace**

Directory where .scala-build is written

**--scalac-help**

Show help for scalac. This is an alias for --scalac-option -help

Aliases: `--help-scalac`

**--scalac-verbose**

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

Aliases: `--verbose-scalac`

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--scala-library**



**--java**

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

**--runner**

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

**--add-stubs**

Add dependency for stubs needed to make $ivy and $dep imports to work.

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--doc-help` ,`--scaladoc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--fmt-help` ,`--scalafmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--json-options**

Command-line options JSON file

</details>

---

## `clean` command
**IMPLEMENTATION specific for Scala Runner specification.**

Clean the workspace.

Passed inputs will establish the Scala CLI project, for which the workspace will be cleaned.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/clean

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--bsp-directory**

Custom BSP configuration location

Aliases: `--bsp-dir`

**--bsp-name**

Name of BSP

Aliases: `--name`

**--workspace**

Directory where .scala-build is written

</details>

---

## `help` command
**IMPLEMENTATION specific for Scala Runner specification.**

Print help message

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

</details>

---

## `install-completions` command
**IMPLEMENTATION specific for Scala Runner specification.**

Aliases: `install-completions`

Installs Scala CLI completions into your shell

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/completions

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--format**

Name of the shell, either zsh or bash

Aliases: `--shell`

**--rc-file**

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

**--output**

Completions output directory

Aliases: `-o`

**--banner**

Custom banner in comment placed in rc file

**--name**

Custom completions name

**--env**

Print completions to stdout

</details>

---

## `install-home` command
**IMPLEMENTATION specific for Scala Runner specification.**

Install Scala CLI in a sub-directory of the home directory

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--scala-cli-binary-path**



**--force**

Overwrite if it exists

Aliases: `-f`

**--binary-name**

Binary name

**--env**

Print the update to `env` variable

**--bin-dir**

Binary directory

</details>

---

## `setup-ide` command
**IMPLEMENTATION specific for Scala Runner specification.**

Generates a BSP file that you can import into your IDE.

You are currently viewing the basic help for the setup-ide sub-command. You can view the full help by running: 
   [1mscala-cli setup-ide --help-full[0m
For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/setup-ide

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `--plugin` ,`-P`

**--scala-version**

Set the Scala version (3.2.2 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-bin` ,`--scala-binary`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--extra-class-path` ,`--class-path` ,`--classpath` ,`-cp` ,`-classpath` ,`--extra-classes` ,`--classes` ,`--extra-class` ,`--class` ,`--extra-jar` ,`--jars` ,`--jar`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `-with-compiler` ,`--with-scala-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--compile-out` ,`--compile-output` ,`--destination` ,`-d` ,`--output-directory`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.12.0 by default).

**--js-mode**

The Scala.js mode, either `dev` or `release`

**--js-module-kind**

The Scala.js module kind: commonjs/common, esmodule/es, nomodule/none

**--js-check-ir**



**--js-emit-source-maps**

Emit source maps

**--js-source-maps-path**

Set the destination path of source maps

**--js-dom**

Enable jsdom

**--js-header**

A header that will be added at the top of generated .js files

**--js-es-version**

The Scala.js ECMA Script version: es5_1, es2015, es2016, es2017, es2018, es2019, es2020, es2021

**--native**

Enable Scala Native. To show more options for Scala Native pass `--help-native`

**--native-version**

Set the Scala Native version (0.4.9 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories

Aliases: `-r` ,`--repo`

**--debug**

Turn debugging on

**--debug-port**

Debug port (5005 by default)

**--debug-mode**

Debug mode (attach by default)

**--java-home**

Set the Java home directory

**--jvm**

Use a specific JVM, such as `14`, `adopt:11`, or `graalvm:21`, or `system`

Aliases: `-j`

**--javac-plugin**

Javac plugin dependencies or files

**--javac-option**

Javac options

Aliases: `--javac-opt`

**--script-snippet**

Allows to execute a passed string as a Scala script

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `-e` ,`--execute-sc` ,`--execute-scala-script`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--extra-compile-only-jar` ,`--compile-only-jars` ,`--compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--extra-source-jar` ,`--source-jars` ,`--source-jar`

**--platform**

Specify platform

**--semantic-db**

Generate SemanticDBs

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--suppress-directives-in-multiple-files-warning**

Suppress warnings about using directives in multiple files

Aliases: `--suppress-warning-directives-in-multiple-files`

**--suppress-outdated-dependency-warning**

Suppress warnings about outdated dependencies in project

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--js-allow-big-ints-for-longs**

Primitive Longs *may* be compiled as primitive JavaScript bigints

**--js-avoid-classes**

Avoid class'es when using functions and prototypes has the same observable semantics.

**--js-avoid-lets-and-consts**

Avoid lets and consts when using vars has the same observable semantics.

**--js-module-split-style**

The Scala.js module split style: fewestmodules, smallestmodules, smallmodulesfor

**--js-small-module-for-package**

Create as many small modules as possible for the classes in the passed packages and their subpackages.

**--js-linker-path**

Path to the Scala.js linker

**--js-cli-version**

Scala.js CLI version to use for linking (1.1.3-sc1 by default).

**--js-cli-java-arg**

Scala.js CLI Java options

**--js-cli-on-jvm**

Whether to run the Scala.js CLI on the JVM or using a native executable

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--native-linking-defaults**

Use default linking settings

**--native-compile-defaults**

Use default compile options

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--jvm-index**

JVM index URL

**--jvm-index-os**

Operating system to use when looking up in the JVM index

**--jvm-index-arch**

CPU architecture to use when looking up in the JVM index

**--bsp-debug-port**

Port for BSP debugging

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--workspace**

Directory where .scala-build is written

**--scalac-help**

Show help for scalac. This is an alias for --scalac-option -help

Aliases: `--help-scalac`

**--scalac-verbose**

Turn verbosity on for scalac. This is an alias for --scalac-option -verbose

Aliases: `--verbose-scalac`

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--scala-library**



**--java**

Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.

**--runner**

Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.

**--add-stubs**

Add dependency for stubs needed to make $ivy and $dep imports to work.

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--doc-help` ,`--scaladoc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--fmt-help` ,`--scalafmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--bsp-directory**

Custom BSP configuration location

Aliases: `--bsp-dir`

**--bsp-name**

Name of BSP

Aliases: `--name`

**--charset**



</details>

---

## `uninstall` command
**IMPLEMENTATION specific for Scala Runner specification.**

Uninstalls Scala CLI.
Works only when installed with the installation script.
For detailed installation instructions refer to our website: https://scala-cli.virtuslab.org/install

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--bloop-bsp-protocol**

Protocol to use to open a BSP connection with Bloop

**--bloop-bsp-socket**

Socket file to use to open a BSP connection with Bloop

**--bloop-host**

Host the compilation server should bind to

**--bloop-port**

Port the compilation server should bind to (pass `-1` to pick a random port)

**--bloop-daemon-dir**

Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)

**--bloop-version**

If Bloop isn't already running, the version we should start

**--bloop-bsp-timeout**

Maximum duration to wait for the BSP connection to be opened

**--bloop-bsp-check-period**

Duration between checks of the BSP connection state

**--bloop-startup-timeout**

Maximum duration to wait for the compilation server to start up

**--bloop-default-java-opts**

Include default JVM options for Bloop

**--bloop-java-opt**

Pass java options to use by Bloop server

**--bloop-global-options-file**

Bloop global options file

**--bloop-jvm**

JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)

**--bloop-working-dir**

Working directory for Bloop, if it needs to be started

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--ttl**

Specify a TTL for changing dependencies, such as snapshots

**--cache**

Set the coursier cache location

**--coursier-validate-checksums**

Enable checksum validation of artifacts downloaded by coursier

**--rc-file**

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

**--banner**

Custom banner in comment placed in rc file

**--name**

Custom completions name

**--force**

Force scala-cli uninstall

Aliases: `-f`

**--skip-cache**

Don't clear Scala CLI cache

**--binary-name**

Binary name

**--bin-dir**

Binary directory

</details>

---

## `uninstall-completions` command
**IMPLEMENTATION specific for Scala Runner specification.**

Aliases: `uninstall-completions`

Uninstalls completions from your shell.

You are currently viewing the basic help for the uninstall completions sub-command. You can view the full help by running: 
   [1mscala-cli uninstall completions --help-full[0m
For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/completions

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--rc-file**

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

**--banner**

Custom banner in comment placed in rc file

**--name**

Custom completions name

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

</details>

---

## `update` command
**IMPLEMENTATION specific for Scala Runner specification.**

Updates Scala CLI.
Works only when installed with the installation script.
If Scala CLI was installed with an external tool, refer to its update methods.

You are currently viewing the basic help for the update sub-command. You can view the full help by running: 
   [1mscala-cli update --help-full[0m
For detailed installation instructions refer to our website: https://scala-cli.virtuslab.org/install

<details><summary>

### Implementantation specific options

</summary>

**--usage**

Print usage and exit

**--help**

Print help message and exit

Aliases: `-h` ,`-help`

**--help-full**

Print help message, including hidden options, and exit

Aliases: `--full-help` ,`-help-full` ,`-full-help`

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-verbose` ,`-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease logging verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--binary-name**

Binary name

**--bin-dir**

Binary directory

**--force**

Force update Scala CLI if it is outdated

Aliases: `-f`

**--is-internal-run**



**--gh-token**

A github token used to access GitHub. Not needed in most cases.

</details>

---

