---
title: Compile
sidebar_position: 5
---

Scala CLI compiles your code with its `compile` command:

```scala title=Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

```bash
scala-cli compile Hello.scala
```

Note that most Scala CLI commands automatically compile your code, if necessary.
The `compile` command is useful if you want to check that your code compiles
(or to see the compilation warnings, if any occur) without running it or packaging it.

The most common `compile` options are shown below.
For a full list of options, run `scala-cli compile --help`, or check the options linked in the
[reference documentation](../reference/commands.md#compile).

## Test scope

`--test` makes `scala-cli` compile main and test scopes:

```bash ignore
scala-cli compile --test Hello.scala
```

## Watch mode

`--watch` makes `scala-cli` watch your code for changes, and re-compiles it upon any change:

```bash ignore
scala-cli compile --watch Hello.scala
# Compiling project-cef76d561e (1 Scala source)
# Compiled 'project-cef76d561e'
# Watching sources, press Ctrl+C to exit.
# Compiling project-cef76d561e (1 Scala source)
# Compiled 'project-cef76d561e'
# Watching sources, press Ctrl+C to exit.
```

## Scala version

Scala CLI uses the latest stable version of Scala which was tested in `scala-cli` (see our list
of [Supported Scala Versions](../reference/scala-versions)). You can specify the Scala version you'd like to use
with `--scala`:

```bash ignore
scala-cli compile --scala 2.13.6 Hello.scala
```

`scala-cli` works with all major `2.12.x`, `2.13.x`, and `3.x` Scala versions.

`--scala` also accepts "short" Scala versions, such as `2.12`, `2`, or `3`. In this
case, it picks the highest corresponding stable Scala version:

```bash ignore
scala-cli compile --scala 2.12 Hello.scala
scala-cli compile --scala 2 Hello.scala
scala-cli compile --scala 3 Hello.scala
```

## Scala Nightlies

The nightly builds of Scala compiler are the unstable ones which are published on a nightly basis.

To use the latest Scala 2 and Scala 3 nightly builds, pass `2.nightly` and `3.nightly`, respectively.
You can also request the last `2.12.nightly` and `2.13.nightly` versions. `2.13.nightly` is the same as `2.nightly`.
Moreover, passing in the `3.{sub binary number}.nightly` format, such as `3.0.nightly` or `3.1.nightly` is accepted,
too.

Scala CLI takes care of fetching the nightly builds of Scala 2 and Scala 3 from different repositories, without you
having to pass their addresses as input after the `--repo` flag.

For compiling with the latest Scala 2 nightly build:

```bash
scala-cli Hello.scala -S 2.nightly
```

For compiling with the latest Scala 3 nightly build:

```bash
scala-cli Hello.scala -S 3.nightly
```

For compiling with a specific nightly build you have the full version:

```bash
scala-cli Hello.scala -S 2.13.9-bin-4505094
```

For setting this inside scala files, use [`using` directives](../guides/using-directives.md):

```scala
//> using scala "2.nightly"
```

```scala
//> using scala "3.nightly"
```

```scala
//> using scala "2.13.9-bin-4505094"
```

## Dependencies

You can add dependencies on the command-line with `--dependency`:

```bash
scala-cli compile Hello.scala \
  --dependency org.scala-lang.modules::scala-parallel-collections:1.0.4
```

Note that `--dependency` is only meant as a convenience. You should favor
adding dependencies in the source files themselves via [`using` directives](../guides/configuration.md#special-imports).

You can also add simple JAR files — those that don’t have transitive dependencies — as dependencies, with `--jar`:

```bash
scala-cli compile Hello.scala --jar /path/to/library.jar
```

See the [Dependency management](../guides/dependencies.md) guide for more details.

## Scala compiler options

### Passing compiler options with `-O`

All [Scala compiler options](https://docs.scala-lang.org/overviews/compiler-options) can be passed to `scala-cli`
with `-O`:

```bash
scala-cli compile Hello.scala -O -deprecation -O -Xlint:infer-any
# [warn] ./Hello.scala:3:7: method x in class Some is deprecated (since 2.12.0): Use .value instead.
# [warn]   opt.x
# [warn]       ^
```

Passing a value to a compiler option requires another `-O`:

```bash
scala-cli -O -Xshow-phases -O -color -O never
```

:::note
Scala CLI uses `bloop` by default, which at times gets in the way of getting the full compiler output.
In the case of some compiler options it may be necessary to turn `bloop` off with `--server=false`.
The Scala CLI team is actively trying to minimize such cases, but for the time being it's a useful workaround.
:::

### Passing compiler options with `using` directives

It is also possible to pass compiler options with the appropriate `using` directives.

A single option can be passed like this:

```scala
//> using option "-new-syntax"
@main def hello = if true then println("Hello")
```

It's also possible to pass a value to the option with the same directive:

```scala
//> using option "-release", "11"

import java.net.http.HttpRequest
```

There's a separate directive for passing multiple options at one time:

```scala
//> using options "-new-syntax", "-rewrite", "-source:3.2-migration"

@main def hello = if (true) println("Hello")
```

### Compiler options recognised even when passed without `-O`

For ease of use many compiler options can be passed as-is to `scala-cli`, without the need of passing after `-O`:

```bash
scala-cli compile Hello.scala -Xlint:infer-any
# Compiling project (1 Scala source)
# [warn] ./Hello.scala:2:11: a type was inferred to be `Any`; this may indicate a programming error.
# [warn]   val l = List("a", true, 2, new Object)
# [warn]           ^
# Compiled project
```

Those include:

- all options which start with:
    - `-g`
    - `-language`
    - `-opt`
    - `-P`
    - `-target`
    - `-source`
    - `-V`
    - `-W`
    - `-X`
    - `-Y`
- the following flags:
    - `-nowarn`
    - `-feature`
    - `-deprecation`
    - `-rewrite`
    - `-old-syntax`
    - `-new-syntax`
    - `-indent`
    - `-no-indent`
- the following options which accept values (which can be passed similarly to any regular `scala-cli` option values)
    - `-encoding`
    - `-release`
    - `-color`
    - `-classpath`
    - `-d`

All options mentioned above are assumed to be Scala compiler options and are being passed as such to the compiler even
without `-O`. (However, they can still be passed with `-O`, regardless.)

:::note
Some compiler options (e.g. `-new-syntax`) may be Scala-version-specific.
Thus, it may happen that Scala CLI will pass those to the compiler,
but they will not be recognised if they're not supported in a given Scala version.
In such a case, refer to the `--scalac-help` output while passing the appropriate version with `-S`.
:::

### Compiler options redirected to Scala CLI alternatives

In a few cases, certain compiler options are being auto-redirected to a corresponding `scala-cli`-specific option for
better integration with other functionalities of the tool.
The redirection happens even when the options are passed with `-O`, making them effectively aliases for their
`scala-cli` counterparts.

Those include:

- `-classpath` being redirected to `--classpath`
- `-d` being redirected to `--compilation-output`

### Scala compiler help

Certain compiler options allow to view relevant help. Inputs aren't required when those are passed.
(since they would be disregarded anyway)

Those include:

- `-help`
- all options prefixed with:
    - `-V`
    - `-W`
    - `-X`
    - `-Y`

```bash
scala-cli -S 2.12.17 -Xshow-phases
#
#     phase name  id  description
#     ----------  --  -----------
#         parser   1  parse source into ASTs, perform simple desugaring
#          namer   2  resolve names, attach symbols to named trees
# packageobjects   3  load package objects
#          typer   4  the meat and potatoes: type the trees
#         patmat   5  translate match expressions
# superaccessors   6  add super accessors in traits and nested classes
#     extmethods   7  add extension methods for inline classes
#        pickler   8  serialize symbol tables
#      refchecks   9  reference/override checking, translate nested objects
#        uncurry  10  uncurry, translate function values to anonymous classes
#         fields  11  synthesize accessors and fields, add bitmaps for lazy vals
#      tailcalls  12  replace tail calls by jumps
#     specialize  13  @specialized-driven class and method specialization
#  explicitouter  14  this refs to outer pointers
#        erasure  15  erase types, add interfaces for traits
#    posterasure  16  clean up erased inline classes
#     lambdalift  17  move nested functions to top level
#   constructors  18  move field definitions into constructors
#        flatten  19  eliminate inner classes
#          mixin  20  mixin composition
#        cleanup  21  platform-specific cleanups, generate reflective calls
#     delambdafy  22  remove lambdas
#            jvm  23  generate JVM bytecode
#       terminal  24  the last phase during a compilation run
```

You can also view the Scala compiler help for a particular Scala version with `--scalac-help`, which is just an alias
for `-O -help`.
Please note that `-help` passed without `-O` will show the `scala-cli` help instead.

```bash
scala-cli -S 2.13.8 --scalac-help
# Usage: scalac <options> <source files>
#
# Standard options:
#   -Dproperty=value             Pass -Dproperty=value directly to the runtime system.
#   -J<flag>                     Pass <flag> directly to the runtime system.
#   -P:<plugin>:<opt>            Pass an option to a plugin
#   -V                           Print a synopsis of verbose options. [false]
#   -W                           Print a synopsis of warning options. [false]
#   -Werror                      Fail the compilation if there are any warnings. [false]
#   -X                           Print a synopsis of advanced options. [false]
#   -Y                           Print a synopsis of private options. [false]
#   -bootclasspath <path>        Override location of bootstrap class files.
#   -classpath <path>            Specify where to find user class files.
#   -d <directory|jar>           destination for generated classfiles.
#   -dependencyfile <file>       Set dependency tracking file.
#   -deprecation                 Emit warning and location for usages of deprecated APIs. See also -Wconf. [false]
#   -encoding <encoding>         Specify character encoding used by source files.
#   -explaintypes                Explain type errors in more detail. [false]
#   -extdirs <path>              Override location of installed extensions.
#   -feature                     Emit warning and location for usages of features that should be imported explicitly. See also -Wconf. [false]
#   -g:<level>                   Set level of generated debugging info. (none,source,line,[vars],notailcalls)
#   -help                        Print a synopsis of standard options [false]
#   -javabootclasspath <path>    Override java boot classpath.
#   -javaextdirs <path>          Override java extdirs classpath.
#   -language:<features>         Enable or disable language features
#   -no-specialization           Ignore @specialize annotations. [false]
#   -nobootcp                    Do not use the boot classpath for the scala jars. [false]
#   -nowarn                      Generate no warnings. [false]
#   -opt:<optimizations>         Enable optimizations, `help` for details.
#   -opt-inline-from:<patterns>  Patterns for classfile names from which to allow inlining, `help` for details.
#   -opt-warnings:<warnings>     Enable optimizer warnings, `help` for details.
#   -print                       Print program with Scala-specific features removed. [false]
#   -release <release>           Compile for a specific version of the Java platform. Supported targets: 6, 7, 8, 9
#   -rootdir <path>              The absolute path of the project root directory, usually the git/scm checkout. Used by -Wconf.
#   -sourcepath <path>           Specify location(s) of source files.
#   -target:<target>             Target platform for object files. ([8],9,10,11,12,13,14,15,16,17,18)
#   -toolcp <path>               Add to the runner classpath.
#   -unchecked                   Enable additional warnings where generated code depends on assumptions. See also -Wconf. [false]
#   -uniqid                      Uniquely tag all identifiers in debugging output. [false]
#   -usejavacp                   Utilize the java.class.path in classpath resolution. [false]
#   -usemanifestcp               Utilize the manifest in classpath resolution. [false]
#   -verbose                     Output messages about what the compiler is doing. [false]
#   -version                     Print product version and exit. [false]
#   @<file>                      A text file containing compiler arguments (options and source files) [false]
#
# Deprecated settings:
#   -optimize                    Enables optimizations. [false]
#                                deprecated: Since 2.12, enables -opt:l:inline -opt-inline-from:**. See -opt:help.
```

## Scala compiler plugins

Use `--compiler-plugin` to add compiler plugin dependencies:

```bash
scala-cli compile Hello.scala --compiler-plugin org.typelevel:::kind-projector:0.13.2 --scala 2.12.14
```

## Printing a class path

`--print-class-path` makes `scala-cli compile` print a class path:

```bash
scala-cli compile --print-class-path Hello.scala
# /work/.scala/project-cef76d561e/classes:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.14/scala-library-2.12.14.jar:~/Library/Caches/ScalaCli/local-repo/0.1.0/org.virtuslab.scala-cli/runner_2.12/0.0.1-SNAPSHOT/jars/runner_2.12.jar:~/Library/Caches/ScalaCli/local-repo/0.1.0/org.virtuslab.scala-cli/stubs/0.0.1-SNAPSHOT/jars/stubs.jar
```

This is handy when working with other tools.
For example, you can pass this class path to `java -cp`:

```bash
java -cp "$(scala-cli compile --print-class-path Hello.scala)" Hello
# Hello
```

Note that you should favor the [`run`](./run.md) command to run your code, rather than running `java -cp`.
The class path obtained this way is only meant for scenarios where `scala-cli` doesn't offer a more convenient option.

### JVM options

`--javac-opt` lets you add `javac` options which will be passed when compiling sources.

```bash
scala-cli Hello.scala --javac-opt source --javac-opt 1.8 --javac-opt target --javac-opt 1.8 
```

You can also add javac options with the using directive `//> using javacOpt`:

```
//> using javacOpt "source", "1.8", "target", "1.8"
```
