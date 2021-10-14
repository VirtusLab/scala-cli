---
title: Compile
sidebar_position: 5
---

Scala CLI can compile your code with its `compile` command:
```bash
cat Hello.scala
# object Hello {
#   def main(args: Array[String]): Unit =
#     println("Hello")
# }
scala-cli compile Hello.scala
```

Note that most commands of the Scala CLI will automatically compile your code if necessary.
The `compile` command is useful if you'd like to simply check that your code compiles,
or know of compilation warnings, without running it or packaging it for example.

We list below some options the `compile` command accepts. For a full list of options,
run `scala compile --help`, or check the options linked in the
[reference documentation](./reference/commands.md#compile).

## Watch mode

`--watch` makes `scala-cli` watch your code for changes, and re-compiles it upon change:
```bash
scala-cli compile --watch Hello.scala
# Compiling project-cef76d561e (1 Scala source)
# Compiled 'project-cef76d561e'
# Watching sources, press Ctrl+C to exit.
# Compiling project-cef76d561e (1 Scala source)
# Compiled 'project-cef76d561e'
# Watching sources, press Ctrl+C to exit.
```

## Scala compiler options

Most [Scala compiler options](https://docs.scala-lang.org/overviews/compiler-options) can be passed as
is to `scala-cli` :
```bash
scala-cli compile Hello.scala -Xlint:infer-any
# Compiling project_b729aa2200-cef76d561e (1 Scala source)
# [warn] ./Hello.scala:2:11: a type was inferred to be `Any`; this may indicate a programming error.
# [warn]   val l = List("a", true, 2, new Object)
# [warn]           ^
# Compiled 'project_b729aa2200-cef76d561e'
```

In more detail, all options starting with
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

Compiler options can also be passed with `-O`:
```bash
scala-cli compile Hello.scala -O -deprecation -O -Xlint:infer-any
# [warn] ./Hello.scala:3:7: method x in class Some is deprecated (since 2.12.0): Use .value instead.
# [warn]   opt.x
# [warn]       ^
```

`-O` accepts both options with the prefixes above and those without such a prefix.

## Scala compiler plugins
Use `--compiler-plugin` to add compiler plugin dependencies:

```bash
scala-cli compile Main.scala --compiler-plugin org.typelevel:::kind-projector:0.13.2 --scala 2.12.14
```

## Scala version

Specify the Scala version you'd like to use with `--scala`:
```bash
scala-cli compile --scala 2.13.6 Hello.scala
```

`scala-cli` should work with all major `2.12.x`, `2.13.x`, and `3.x` Scala versions.

`--scala` also accepts "short" Scala versions, such as `2.12`, `2`, or `3`. In that
case, it picks the highest corresponding stable Scala version:
```bash
scala-cli compile --scala 2.12 Hello.scala
scala-cli compile --scala 2 Hello.scala
scala-cli compile --scala 3 Hello.scala
```

## Dependencies

You can add dependencies on the command-line, via `--dependency`:
```bash
scala-cli compile Hello.scala --dependency dev.zio::zio:1.0.9
```

Note that `--dependency` is only meant as a convenience. You should favour
adding dependencies in the sources themselves (via `import $dep`)
or in a configuration file.

You can also add repositories on the command-line, via `--repository`:
```bash
scala-cli compile Hello.scala --dependency com.pany::util:33.1.0 --repo https://artifacts.pany.com/maven
```

Lastly, you can also add simple JAR files as dependencies, with `--jar`:
```bash
scala-cli compile Hello.scala --jar /path/to/library.jar
```

## Printing a class path

`--class-path` makes `scala compile` print a class path that can be passed to other tools:
```bash
scala-cli compile --class-path Hello.scala
# /work/.scala/project-cef76d561e/classes:~/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.14/scala-library-2.12.14.jar:~/Library/Caches/ScalaCli/local-repo/0.1.0/org.virtuslab.scala-cli/runner_2.12/0.0.1-SNAPSHOT/jars/runner_2.12.jar:~/Library/Caches/ScalaCli/local-repo/0.1.0/org.virtuslab.scala-cli/stubs/0.0.1-SNAPSHOT/jars/stubs.jar
```

You can pass this class path to `java -cp` for example:
```bash
java -cp "$(scala compile --class-path Hello.scala)" Hello
# Hello
```

Note that you should favour the [`run`](./run.md) command to run your code, rather than running `java -cp`.
The class path obtained this way is meant for scenarios where `scala-cli` doesn't offer a more
convenient option.
