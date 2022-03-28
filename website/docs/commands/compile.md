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
The `compile` command is useful if you'd like to check that your code compiles,
or know of compilation warnings, without running it or packaging it.

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

Scala CLI uses the latest stable version of Scala which was tested in `scala-cli` (see our list of [Supported Scala Versions](../reference/scala-versions)). You can specify the Scala version you'd like to use with `--scala`:

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

The nightly builds of Scala compiler are unstable ones which are published on a nightly basis.

To use the latest Scala 2 and Scala 3 nightly builds, pass `2.nightly` and `3.nightly`, respectively. 
You can also request the last `2.12.nightly` and `2.13.nightly` versions. `2.13.nightly` is the same as `2.nightly`.
Moreover, passing the `3.{sub binary number}.nightly` format, such as `3.0.nightly` or `3.1.nightly` is accepted, too.

Scala CLI takes care of fetching the nightly builds of Scala 2 and Scala 3 from different repositories, without you having to pass their addresses as input after the `--repo` flag.

For compiling with the latest Scala 2 nightly build: 
```bash
scala-cli Hello.scala -S 2.nightly
```
For compiling with the latest Scala 3 nightly build:
```bash
scala-cli Hello.scala -S 3.nightly
```
For compiling with an specific nightly build, you have the full version for:
```bash
scala-cli Hello.scala -S 2.13.9-bin-4505094 
```

For adding this inside scala files with [using directives](../guides/using-directives.md), use:

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

Most [Scala compiler options](https://docs.scala-lang.org/overviews/compiler-options) can be passed as-is to `scala-cli`:

```bash
scala-cli compile Hello.scala -Xlint:infer-any
# Compiling project_b729aa2200-cef76d561e (1 Scala source)
# [warn] ./Hello.scala:2:11: a type was inferred to be `Any`; this may indicate a programming error.
# [warn]   val l = List("a", true, 2, new Object)
# [warn]           ^
# Compiled 'project_b729aa2200-cef76d561e'
```

All `scala-cli` options that start with:

- `-g`
- `-language`
- `-opt`
- `-P`
- `-target`
- `-V`
- `-W`
- `-X`
- `-Y`

are assumed to be Scala compiler options.

Scala compiler options can also be passed with `-O`:

```bash
scala-cli compile Hello.scala -O -deprecation -O -Xlint:infer-any
# [warn] ./Hello.scala:3:7: method x in class Some is deprecated (since 2.12.0): Use .value instead.
# [warn]   opt.x
# [warn]       ^
```

`-O` accepts both options with the prefixes shown above, and those without such a prefix.

## Scala compiler plugins
Use `--compiler-plugin` to add compiler plugin dependencies:

```bash
scala-cli compile Hello.scala --compiler-plugin org.typelevel:::kind-projector:0.13.2 --scala 2.12.14
```


## Printing a class path

`--class-path` makes `scala-cli compile` print a class path:
```bash
scala-cli compile --class-path Hello.scala
# /work/.scala/project-cef76d561e/classes:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.14/scala-library-2.12.14.jar:~/Library/Caches/ScalaCli/local-repo/0.1.0/org.virtuslab.scala-cli/runner_2.12/0.0.1-SNAPSHOT/jars/runner_2.12.jar:~/Library/Caches/ScalaCli/local-repo/0.1.0/org.virtuslab.scala-cli/stubs/0.0.1-SNAPSHOT/jars/stubs.jar
```

This is handy when working with other tools.
For example, you can pass this class path to `java -cp`:
```bash
java -cp "$(scala-cli compile --class-path Hello.scala)" Hello
# Hello
```

Note that you should favor the [`run`](./run.md) command to run your code, rather than running `java -cp`.
The class path obtained this way is only meant for scenarios where `scala-cli` doesn't offer a more convenient option.
