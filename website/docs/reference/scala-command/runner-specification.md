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

Compile Scala code

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--scala-version**

Set the Scala version (3.2.1 by default)

Aliases: `--scala` ,`-S`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--output-directory` ,`-d` ,`--destination` ,`--compile-output` ,`--compile-out`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.10.1 by default).

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

Set the Scala Native version (0.4.8 by default).

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

Aliases: `--repo` ,`-r`

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

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

**--platform**

Specify platform

**--watch**

Watch source files for changes

Aliases: `-w`

**--restart**

Run your application in background and automatically restart if sources have been changed

Aliases: `--revolver`

**--test**

Compile test scope

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

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

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `--execute-scala-script` ,`--execute-sc` ,`-e`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--scaladoc-help` ,`--doc-help` ,`--help-doc`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--scalafmt-help` ,`--fmt-help` ,`--help-fmt`

</details>

---

## `doc` command
**MUST have for Scala Runner specification.**

Generate Scaladoc documentation

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--scala-version**

Set the Scala version (3.2.1 by default)

Aliases: `--scala` ,`-S`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--output-directory` ,`-d` ,`--destination` ,`--compile-output` ,`--compile-out`

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

The Scala.js version (1.10.1 by default).

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

Set the Scala Native version (0.4.8 by default).

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

Aliases: `--repo` ,`-r`

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

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

**--platform**

Specify platform

**--default-scaladoc-options**

Control if Scala CLI should use default options for scaladoc, true by default. Use `--default-scaladoc-opts:false` to not include default options.

Aliases: `--default-scaladoc-opts`

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

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

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `--execute-scala-script` ,`--execute-sc` ,`-e`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--scaladoc-help` ,`--doc-help` ,`--help-doc`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--scalafmt-help` ,`--fmt-help` ,`--help-fmt`

</details>

---

## `repl` command
**MUST have for Scala Runner specification.**

Aliases: `console`

Fire-up a Scala REPL

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--scala-version**

Set the Scala version (3.2.1 by default)

Aliases: `--scala` ,`-S`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--output-directory` ,`-d` ,`--destination` ,`--compile-output` ,`--compile-out`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

**--java-prop**

Set Java properties

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.10.1 by default).

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

Set the Scala Native version (0.4.8 by default).

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

Aliases: `--repo` ,`-r`

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

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

**--platform**

Specify platform

**--watch**

Watch source files for changes

Aliases: `-w`

**--restart**

Run your application in background and automatically restart if sources have been changed

Aliases: `--revolver`

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

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

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `--execute-scala-script` ,`--execute-sc` ,`-e`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--scaladoc-help` ,`--doc-help` ,`--help-doc`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--scalafmt-help` ,`--fmt-help` ,`--help-fmt`

</details>

---

## `run` command
**MUST have for Scala Runner specification.**

Compile and run Scala code.

To pass arguments to the application, just add them after `--`, like:

```sh
scala-cli MyApp.scala -- first-arg second-arg
```

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--scala-version**

Set the Scala version (3.2.1 by default)

Aliases: `--scala` ,`-S`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--output-directory` ,`-d` ,`--destination` ,`--compile-output` ,`--compile-out`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

**--java-prop**

Set Java properties

**--main-class**

Specify which main class to run

Aliases: `-M`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.10.1 by default).

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

Set the Scala Native version (0.4.8 by default).

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

Aliases: `--repo` ,`-r`

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

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

**--platform**

Specify platform

**--watch**

Watch source files for changes

Aliases: `-w`

**--restart**

Run your application in background and automatically restart if sources have been changed

Aliases: `--revolver`

**--main-class-ls**

List main classes available in the current context

Aliases: `--main-class-list` ,`--list-main-class` ,`--list-main-classes`

**--command**

Print the command that would have been run (one argument per line), rather than running it

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

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

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `--execute-scala-script` ,`--execute-sc` ,`-e`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--scaladoc-help` ,`--doc-help` ,`--help-doc`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--scalafmt-help` ,`--fmt-help` ,`--help-fmt`

</details>

---

## `shebang` command
**MUST have for Scala Runner specification.**

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



### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--scala-version**

Set the Scala version (3.2.1 by default)

Aliases: `--scala` ,`-S`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--output-directory` ,`-d` ,`--destination` ,`--compile-output` ,`--compile-out`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

**--java-prop**

Set Java properties

**--main-class**

Specify which main class to run

Aliases: `-M`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.10.1 by default).

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

Set the Scala Native version (0.4.8 by default).

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

Aliases: `--repo` ,`-r`

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

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

**--platform**

Specify platform

**--watch**

Watch source files for changes

Aliases: `-w`

**--restart**

Run your application in background and automatically restart if sources have been changed

Aliases: `--revolver`

**--main-class-ls**

List main classes available in the current context

Aliases: `--main-class-list` ,`--list-main-class` ,`--list-main-classes`

**--command**

Print the command that would have been run (one argument per line), rather than running it

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

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

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `--execute-scala-script` ,`--execute-sc` ,`-e`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--scaladoc-help` ,`--doc-help` ,`--help-doc`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--scalafmt-help` ,`--fmt-help` ,`--help-fmt`

</details>

---

# SHOULD have commands

## `fmt` command
**SHOULD have for Scala Runner specification.**

Aliases: `format`, `scalafmt`

Format Scala code

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--scala-version**

Set the Scala version (3.2.1 by default)

Aliases: `--scala` ,`-S`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--output-directory` ,`-d` ,`--destination` ,`--compile-output` ,`--compile-out`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.10.1 by default).

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

Set the Scala Native version (0.4.8 by default).

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

Aliases: `--repo` ,`-r`

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

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

**--platform**

Specify platform

**--check**

Check if sources are well formatted

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

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

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `--execute-scala-script` ,`--execute-sc` ,`-e`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--scaladoc-help` ,`--doc-help` ,`--help-doc`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--scalafmt-help` ,`--fmt-help` ,`--help-fmt`

**--respect-project-filters**

Use project filters defined in the configuration. Turned on by default, use `--respect-project-filters:false` to disable it.

**--save-scalafmt-conf**

Saves .scalafmt.conf file if it was created or overwritten

**--scalafmt-conf**

Custom path to the scalafmt configuration file.

Aliases: `--scalafmt-config`

**--scalafmt-dialect**

Pass a global dialect for scalafmt. This overrides whatever value is configured in the .scalafmt.conf file or inferred based on Scala version used.

Aliases: `--dialect`

**--scalafmt-version**

Pass scalafmt version before running it (3.5.9 by default). If passed, this overrides whatever value is configured in the .scalafmt.conf file.

Aliases: `--fmt-version`

</details>

---

## `test` command
**SHOULD have for Scala Runner specification.**

Compile and test Scala code

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--scala-version**

Set the Scala version (3.2.1 by default)

Aliases: `--scala` ,`-S`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--output-directory` ,`-d` ,`--destination` ,`--compile-output` ,`--compile-out`

**--java-opt**

Set Java options, such as `-Xmx1g`

Aliases: `-J`

**--java-prop**

Set Java properties

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.10.1 by default).

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

Set the Scala Native version (0.4.8 by default).

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

Aliases: `--repo` ,`-r`

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

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

**--platform**

Specify platform

**--watch**

Watch source files for changes

Aliases: `-w`

**--restart**

Run your application in background and automatically restart if sources have been changed

Aliases: `--revolver`

**--test-framework**

Name of the test framework's runner class to use while running tests

**--require-tests**

Fail if no test suites were run

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

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

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `--execute-scala-script` ,`--execute-sc` ,`-e`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--scaladoc-help` ,`--doc-help` ,`--help-doc`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--scalafmt-help` ,`--fmt-help` ,`--help-fmt`

</details>

---

## `version` command
**SHOULD have for Scala Runner specification.**

Print version

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--cli-version**

Show only plain version

Aliases: `--cli`

**--scala-version**

Show only plain scala version

Aliases: `--scala`

</details>

---

# IMPLEMENTATION specific commands

## `about` command
**IMPLEMENTATION specific for Scala Runner specification.**

Print details about this application

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

</details>

---

## `bsp` command
**IMPLEMENTATION specific for Scala Runner specification.**

Start BSP server

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--scala-version**

Set the Scala version (3.2.1 by default)

Aliases: `--scala` ,`-S`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--output-directory` ,`-d` ,`--destination` ,`--compile-output` ,`--compile-out`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.10.1 by default).

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

Set the Scala Native version (0.4.8 by default).

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

Aliases: `--repo` ,`-r`

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

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

**--platform**

Specify platform

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

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

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `--execute-scala-script` ,`--execute-sc` ,`-e`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--scaladoc-help` ,`--doc-help` ,`--help-doc`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--scalafmt-help` ,`--fmt-help` ,`--help-fmt`

</details>

---

## `clean` command
**IMPLEMENTATION specific for Scala Runner specification.**

Clean the workspace

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

</details>

---

## `doctor` command
**IMPLEMENTATION specific for Scala Runner specification.**

Print details about this application

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--gh-token**

A github token used to access GitHub. Not needed in most cases.

</details>

---

## `help` command
**IMPLEMENTATION specific for Scala Runner specification.**

Print help message

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

</details>

---

## `install-completions` command
**IMPLEMENTATION specific for Scala Runner specification.**

Aliases: `install-completions`

Installs completions into your shell

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--format**

Name of the shell, either zsh or bash

Aliases: `--shell`

**--rc-file**

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

**--output**

Completions output directory

Aliases: `-o`

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

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--scala-cli-binary-path**



**--force**

Overwrite if it exists

Aliases: `-f`

**--env**

Print the update to `env` variable

</details>

---

## `setup-ide` command
**IMPLEMENTATION specific for Scala Runner specification.**

Generate a BSP file that you can import into your IDE

### MUST have options

**--dependency**

Add dependencies

Aliases: `--dep`

**--compiler-plugin**

Add compiler plugin dependencies

Aliases: `-P` ,`--plugin`

**--scalac-option**

Add a `scalac` option. Note that options starting with `-g`, `-language`, `-opt`, `-P`, `-target`, `-V`, `-W`, `-X`, and `-Y` are assumed to be Scala compiler options and don't require to be passed after `-O` or `--scalac-option`.

Aliases: `--scala-opt` ,`-O` ,`--scala-option`

**--scala-version**

Set the Scala version (3.2.1 by default)

Aliases: `--scala` ,`-S`

**--extra-jars**

Add extra JARs and compiled classes to the class path

Aliases: `--jar` ,`--jars` ,`--extra-jar` ,`--class` ,`--extra-class` ,`--classes` ,`--extra-classes` ,`-classpath` ,`-cp` ,`--classpath` ,`--class-path` ,`--extra-class-path`

**--resource-dirs**

Add a resource directory

Aliases: `--resource-dir`

**--compilation-output**

Copy compilation results to output directory using either relative or absolute path

Aliases: `--output-directory` ,`-d` ,`--destination` ,`--compile-output` ,`--compile-out`

### SHOULD have options

**--js**

Enable Scala.js. To show more options for Scala.js pass `--help-js`

**--js-version**

The Scala.js version (1.10.1 by default).

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

Set the Scala Native version (0.4.8 by default).

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

Aliases: `--repo` ,`-r`

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

**--extra-compile-only-jars**

Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.

Aliases: `--compile-only-jar` ,`--compile-only-jars` ,`--extra-compile-only-jar`

**--extra-source-jars**

Add extra source JARs

Aliases: `--source-jar` ,`--source-jars` ,`--extra-source-jar`

**--platform**

Specify platform

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

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

**--native-clang**

Path to the Clang command

**--native-clangpp**

Path to the Clang++ command

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--execute-script**

A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

Aliases: `--execute-scala-script` ,`--execute-sc` ,`-e`

**--scala-snippet**

Allows to execute a passed string as Scala code

**--execute-scala**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--java-snippet**

Allows to execute a passed string as Java code

**--execute-java**

A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly

**--help-js**

Show options for ScalaJS

**--help-native**

Show options for ScalaNative

**--help-scaladoc**

Show options for Scaladoc

Aliases: `--scaladoc-help` ,`--doc-help` ,`--help-doc`

**--help-repl**

Show options for Scala REPL

Aliases: `--repl-help`

**--help-scalafmt**

Show options for Scalafmt

Aliases: `--scalafmt-help` ,`--fmt-help` ,`--help-fmt`

</details>

---

## `uninstall` command
**IMPLEMENTATION specific for Scala Runner specification.**

Uninstall scala-cli - only works when installed by the installation script

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--server**

Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.

**--home-directory**

Override the path to user's home directory

Aliases: `--home`

**--rc-file**

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

**--force**

Force scala-cli uninstall

Aliases: `-f`

</details>

---

## `uninstall-completions` command
**IMPLEMENTATION specific for Scala Runner specification.**

Aliases: `uninstall-completions`

Uninstalls completions from your shell

<details><summary>

### Implementantation specific options

</summary>

**--rc-file**

Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

</details>

---

## `update` command
**IMPLEMENTATION specific for Scala Runner specification.**

Update scala-cli - only works when installed by the installation script

<details><summary>

### Implementantation specific options

</summary>

**--verbose**

Increase verbosity (can be specified multiple times)

Aliases: `-v`

**--interactive**

Interactive mode

Aliases: `-i`

**--actions**

Enable actionable diagnostics

**--quiet**

Decrease verbosity

Aliases: `-q`

**--progress**

Use progress bars

**--force**

Force update scala-cli if is outdated

Aliases: `-f`

</details>

---

