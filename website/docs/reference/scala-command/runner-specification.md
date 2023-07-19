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

**--compile-only-dependency**

Add compile-only dependencies

Aliases: `--compile-dep` ,`--compile-lib`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scala-version**

Set the Scala version (3.3.0 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-binary` ,`--scala-bin`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `--with-scala-compiler` ,`-with-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `-d` ,`--output-directory` ,`--destination` ,`--compile-output` ,`--compile-out`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.13.1 by default).

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

Set the Scala Native version (0.4.14 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-lto**

Link-time optimisation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

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

Aliases: `-e` ,`--execute-scala-script` ,`--execute-sc`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

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

Aliases: `-p` ,`--print-classpath`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Scala.js CLI version to use for linking (1.13.1 by default).

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

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--scaladoc-help` ,`--doc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--scalafmt-help` ,`--fmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--exclude**

Exclude sources

</details>

---

## `config` command
**MUST have for Scala Runner specification.**

Configure global settings for Scala CLI.

Syntax:
```sh
  scala-cli config key value
```
For example, to globally set the interactive mode:
```sh
  scala-cli config interactive true
```
  
Available keys:
  - actions                                        Globally enables actionable diagnostics. Enabled by default.
  - github.token                                   GitHub token.
  - httpProxy.address                              HTTP proxy address.
  - httpProxy.password                             HTTP proxy password (used for authentication).
  - httpProxy.user                                 HTTP proxy user (used for authentication).
  - interactive                                    Globally enables interactive mode (the '--interactive' flag).
  - interactive-was-suggested                      Setting indicating if the global interactive mode was already suggested.
  - pgp.public-key                                 The PGP public key, used for signing.
  - pgp.secret-key                                 The PGP secret key, used for signing.
  - pgp.secret-key-password                        The PGP secret key password, used for signing.
  - power                                          Globally enables power mode (the '--power' launcher flag).
  - publish.credentials                            Publishing credentials, syntax: repositoryAddress value:user value:password [realm]
  - publish.user.email                             The 'email' user detail, used for publishing.
  - publish.user.name                              The 'name' user detail, used for publishing.
  - publish.user.url                               The 'url' user detail, used for publishing.
  - repositories.credentials                       Repository credentials, syntax: repositoryAddress value:user value:password [realm]
  - repositories.default                           Default repository, syntax: https://first-repo.company.com https://second-repo.company.com
  - repositories.mirrors                           Repository mirrors, syntax: repositories.mirrors maven:*=https://repository.company.com/maven
  - suppress-warning.directives-in-multiple-files  Globally suppresses warnings about directives declared in multiple source files.
  - suppress-warning.experimental-features         Globally suppresses warnings about experimental features.
  - suppress-warning.outdated-dependencies-files   Globally suppresses warnings about outdated dependencies.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/config

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

**--unset**

Remove an entry from config

Aliases: `--remove`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

**--dump**

Dump config DB as JSON

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

**--compile-only-dependency**

Add compile-only dependencies

Aliases: `--compile-dep` ,`--compile-lib`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scala-version**

Set the Scala version (3.3.0 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-binary` ,`--scala-bin`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `--with-scala-compiler` ,`-with-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `-d` ,`--output-directory` ,`--destination` ,`--compile-output` ,`--compile-out`

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

The Scala.js version (1.13.1 by default).

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

Set the Scala Native version (0.4.14 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-lto**

Link-time optimisation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

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

Aliases: `-e` ,`--execute-scala-script` ,`--execute-sc`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Scala.js CLI version to use for linking (1.13.1 by default).

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

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--scaladoc-help` ,`--doc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--scalafmt-help` ,`--fmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--exclude**

Exclude sources

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

**--compile-only-dependency**

Add compile-only dependencies

Aliases: `--compile-dep` ,`--compile-lib`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scala-version**

Set the Scala version (3.3.0 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-binary` ,`--scala-bin`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `--with-scala-compiler` ,`-with-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `-d` ,`--output-directory` ,`--destination` ,`--compile-output` ,`--compile-out`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.13.1 by default).

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

Set the Scala Native version (0.4.14 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-lto**

Link-time optimisation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

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

Aliases: `-e` ,`--execute-scala-script` ,`--execute-sc`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Scala.js CLI version to use for linking (1.13.1 by default).

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

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--scaladoc-help` ,`--doc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--scalafmt-help` ,`--fmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--exclude**

Exclude sources

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

Specific run configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

For a run to be successful, a main method must be present on the classpath.
.sc scripts are an exception, as a main class is provided in their wrapper.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

To pass arguments to the actual application, just add them after `--`, like:
```sh
  scala-cli run Main.scala AnotherSource.scala -- first-arg second-arg
```

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/run

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compile-only-dependency**

Add compile-only dependencies

Aliases: `--compile-dep` ,`--compile-lib`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scala-version**

Set the Scala version (3.3.0 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-binary` ,`--scala-bin`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `--with-scala-compiler` ,`-with-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `-d` ,`--output-directory` ,`--destination` ,`--compile-output` ,`--compile-out`

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

The Scala.js version (1.13.1 by default).

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

Set the Scala Native version (0.4.14 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-lto**

Link-time optimisation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

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

Aliases: `-e` ,`--execute-scala-script` ,`--execute-sc`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Scala.js CLI version to use for linking (1.13.1 by default).

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

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--scaladoc-help` ,`--doc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--scalafmt-help` ,`--fmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--exclude**

Exclude sources

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

This command is equivalent to the `run` sub-command, but it changes the way
Scala CLI parses its command-line arguments in order to be compatible
with shebang scripts.

When relying on the `run` sub-command, inputs and scala-cli options can be mixed,
while program args have to be specified after `--`
```sh
  scala-cli [command] [scala-cli_options | input]... -- [program_arguments]...
```

However, for the `shebang` sub-command, only a single input file can be set, while all scala-cli options
have to be set before the input file.
All inputs after the first are treated as program arguments, without the need for `--`
```sh
  scala-cli shebang [scala-cli_options]... input [program_arguments]...
```

Using this, it is possible to conveniently set up Unix shebang scripts. For example:
```scala
  #!/usr/bin/env -S scala-cli shebang --scala-version 2.13
  println("Hello, world")
```

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/shebang

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compile-only-dependency**

Add compile-only dependencies

Aliases: `--compile-dep` ,`--compile-lib`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scala-version**

Set the Scala version (3.3.0 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-binary` ,`--scala-bin`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `--with-scala-compiler` ,`-with-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `-d` ,`--output-directory` ,`--destination` ,`--compile-output` ,`--compile-out`

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

The Scala.js version (1.13.1 by default).

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

Set the Scala Native version (0.4.14 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-lto**

Link-time optimisation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

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

Aliases: `-e` ,`--execute-scala-script` ,`--execute-sc`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Scala.js CLI version to use for linking (1.13.1 by default).

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

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--scaladoc-help` ,`--doc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--scalafmt-help` ,`--fmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--exclude**

Exclude sources

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

**--compile-only-dependency**

Add compile-only dependencies

Aliases: `--compile-dep` ,`--compile-lib`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scala-version**

Set the Scala version (3.3.0 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-binary` ,`--scala-bin`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `--with-scala-compiler` ,`-with-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `-d` ,`--output-directory` ,`--destination` ,`--compile-output` ,`--compile-out`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.13.1 by default).

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

Set the Scala Native version (0.4.14 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-lto**

Link-time optimisation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

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

Aliases: `-e` ,`--execute-scala-script` ,`--execute-sc`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Scala.js CLI version to use for linking (1.13.1 by default).

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

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--scaladoc-help` ,`--doc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--scalafmt-help` ,`--fmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--exclude**

Exclude sources

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

Pass an argument to scalafmt.

Aliases: `-F`

**--scalafmt-conf**

Custom path to the scalafmt configuration file.

Aliases: `--scalafmt-config`

**--scalafmt-conf-str**

Pass configuration as a string.

Aliases: `--scalafmt-config-str` ,`--scalafmt-conf-snippet`

**--scalafmt-dialect**

Pass a global dialect for scalafmt. This overrides whatever value is configured in the .scalafmt.conf file or inferred based on Scala version used.

Aliases: `--dialect`

**--scalafmt-version**

Pass scalafmt version before running it (3.7.10 by default). If passed, this overrides whatever value is configured in the .scalafmt.conf file.

Aliases: `--fmt-version`

</details>

---

## `test` command
**SHOULD have for Scala Runner specification.**

Compile and test Scala code.

Test sources are compiled separately (after the 'main' sources), and may use different dependencies, compiler options, and other configurations.
A source file is treated as a test source if:
  - the file name ends with `.test.scala`
  - the file comes from a directory that is provided as input, and the relative path from that file to its original directory contains a `test` directory
  - it contains the `//> using target.scope "test"` directive (Experimental)

Specific test configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

Multiple inputs can be passed at once.
Paths to directories, URLs and supported file types are accepted as inputs.
Accepted file extensions: .scala, .sc, .java, .jar, .md, .jar, .c, .h, .zip
For piped inputs use the corresponding alias: _.scala, _.java, _.sc, _.md
All supported types of inputs can be mixed with each other.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/test

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compile-only-dependency**

Add compile-only dependencies

Aliases: `--compile-dep` ,`--compile-lib`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scala-version**

Set the Scala version (3.3.0 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-binary` ,`--scala-bin`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `--with-scala-compiler` ,`-with-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `-d` ,`--output-directory` ,`--destination` ,`--compile-output` ,`--compile-out`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.13.1 by default).

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

Set the Scala Native version (0.4.14 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-lto**

Link-time optimisation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

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

Aliases: `-e` ,`--execute-scala-script` ,`--execute-sc`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Scala.js CLI version to use for linking (1.13.1 by default).

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

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--scaladoc-help` ,`--doc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--scalafmt-help` ,`--fmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--exclude**

Exclude sources

**--java-prop-option**

Add java properties. Note that options equal `-Dproperty=value` are assumed to be java properties and don't require to be passed after `--java-prop`.

Aliases: `--java-prop`

</details>

---

## `version` command
**SHOULD have for Scala Runner specification.**

Prints the version of the Scala CLI and the default version of Scala. (which can be overridden in the project)
If network connection is available, this sub-command also checks if the installed Scala CLI is up-to-date.

The version of the Scala CLI is the version of the command-line tool that runs Scala programs, which
is distinct from the Scala version of the compiler. We recommend to specify the version of the Scala compiler
for a project in its sources (via a using directive). Otherwise, Scala CLI falls back to the default
Scala version defined by the runner.

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

**--compile-only-dependency**

Add compile-only dependencies

Aliases: `--compile-dep` ,`--compile-lib`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scala-version**

Set the Scala version (3.3.0 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-binary` ,`--scala-bin`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `--with-scala-compiler` ,`-with-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `-d` ,`--output-directory` ,`--destination` ,`--compile-output` ,`--compile-out`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.13.1 by default).

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

Set the Scala Native version (0.4.14 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-lto**

Link-time optimisation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

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

Aliases: `-e` ,`--execute-scala-script` ,`--execute-sc`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Scala.js CLI version to use for linking (1.13.1 by default).

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

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--scaladoc-help` ,`--doc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--scalafmt-help` ,`--fmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--exclude**

Exclude sources

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

The setup-ide sub-command allows to pre-configure a Scala CLI project to import to an IDE with BSP support.
It is also ran implicitly when `compile`, `run`, `shebang` or `test` sub-commands are called.

The pre-configuration should be saved in a BSP json connection file under the path:
```sh
    {project-root}/.bsp/scala-cli.json
```

Specific setup-ide configurations can be specified with both command line options and using directives defined in sources.
Command line options always take priority over using directives when a clash occurs, allowing to override configurations defined in sources.
Using directives can be defined in all supported input source file types.

For detailed documentation refer to our website: https://scala-cli.virtuslab.org/docs/commands/setup-ide

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compile-only-dependency**

Add compile-only dependencies

Aliases: `--compile-dep` ,`--compile-lib`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scala-version**

Set the Scala version (3.3.0 by default)

Aliases: `-S` ,`--scala`

**--scala-binary-version**

Set the Scala binary version

Aliases: `-B` ,`--scala-binary` ,`--scala-bin`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--with-compiler**

Allows to include the Scala compiler artifacts on the classpath.

Aliases: `--with-scala-compiler` ,`-with-compiler`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `-d` ,`--output-directory` ,`--destination` ,`--compile-output` ,`--compile-out`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.13.1 by default).

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

Set the Scala Native version (0.4.14 by default).

**--native-mode**

Set Scala Native compilation mode

**--native-lto**

Link-time optimisation mode

**--native-gc**

Set the Scala Native garbage collector

**--native-linking**

Extra options passed to `clang` verbatim during linking

**--native-compile**

List of compile options

**--embed-resources**

Embed resources into the Scala Native binary (can be read with the Java resources API)

**--repository**

Add repositories for dependency resolution.

Accepts predefined repositories supported by Coursier (like `sonatype:snapshots` or `m2Local`) or a URL of the root of Maven repository

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

Aliases: `-e` ,`--execute-scala-script` ,`--execute-sc`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Scala.js CLI version to use for linking (1.13.1 by default).

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

**--default-forbidden-directories**



**--forbid**



**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--help-doc` ,`--scaladoc-help` ,`--doc-help`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--help-fmt` ,`--scalafmt-help` ,`--fmt-help`

**--strict-bloop-json-check**



**--with-toolkit**

Add toolkit to classPath

Aliases: `--toolkit`

**--exclude**

Exclude sources

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

Uninstalls Scala CLI completions from your shell.

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

</details>

---

## `update` command
**IMPLEMENTATION specific for Scala Runner specification.**

Updates Scala CLI.
Works only when installed with the installation script.
If Scala CLI was installed with an external tool, refer to its update methods.

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

**--suppress-experimental-feature-warning**

Suppress warnings about using experimental features

Aliases: `--suppress-experimental-warning`

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

