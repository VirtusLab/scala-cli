---
title: Managing dependencies
sidebar_position: 9
---

# Managing dependencies

## Dependency syntax

Dependencies are declared in Scala CLI according to the following format:

```text
groupID:artifactID:revision
```

This is similar to how you declare dependencies in SBT with the `%` character.
For example:

```text
org.scala-lang.modules:scala-parallel-collections_2.13:1.0.4
```

You can also skip explicitly stating the Scala version in the artifact name by repeating the `:` character after
the `groupID` (similarly to how you can do the same with `%%` in SBT). This is just a shortcut, Scala CLI will still add
the Scala version for you when fetching the dependency. Also, this only applies to Scala dependencies.

```text
org.scala-lang.modules::scala-parallel-collections:1.0.4
```

Java and other non-scala dependencies follow the same syntax (without the `::` for implicit Scala version, of course).
For example:
```text
org.postgresql:postgresql:42.2.8
```

## Specifying dependencies from the command line

You can add dependencies on the command line, with the `--dependency` option:

```bash
scala-cli compile Hello.scala \
  --dependency org.scala-lang.modules::scala-parallel-collections:1.0.4
```

You can also add a URL fallback for a JAR dependency, if it can't be fetched otherwise:

```bash
scala-cli compile Hello.scala \
  -- dependency "org::name::version,url=https://url-to-the-jar"
```

Note that `--dependency` is only meant as a convenience. You should favor adding dependencies in the sources themselves
via [using directives](/docs/guides/configuration.md#special-imports). However, the `--dependency` CLI option takes
precedence over `using` directives, so it can be used to override a `using` directive, such as when you want to work
with a different dependency version.

You can also add repositories on the command-line, via `--repository`:

```bash
scala-cli compile Hello.scala \
  --dependency com.pany::util:33.1.0 --repo https://artifacts.pany.com/maven
```

Lastly, you can also add simple JAR files as dependencies with `--jar`:

```bash
scala-cli compile Hello.scala --jar /path/to/library.jar
```

## Updating dependencies

To check if dependencies in using directives are up-to-date, use `dependency-update` command:

```scala title=Hello.scala
//> using lib "com.lihaoyi::os-lib:0.7.8"
//> using lib "com.lihaoyi::utest:0.7.10"
import $ivy.`com.lihaoyi::geny:0.6.5`
import $dep.`com.lihaoyi::pprint:0.6.6`

object Hello extends App {
  println("Hello World")
}
```

```bash
scala-cli dependency-update Hello.scala
# Updates
#    * com.lihaoyi::os-lib:0.7.8 -> 0.8.1
#    * com.lihaoyi::utest:0.7.10 -> 0.8.0
#    * com.lihaoyi::geny:0.6.5 -> 0.7.1
#    * com.lihaoyi::pprint:0.6.6 -> 0.7.3
# To update all dependencies run: 
#     scala-cli dependency-update --all
```

Passing `--all` to the `dependency-update` sub-command updates all dependencies in your sources.

```bash
scala-cli dependency-update Hello.scala --all
# Updated dependency to: com.lihaoyi::os-lib:0.8.1
# Updated dependency to: com.lihaoyi::utest:0.8.0
# Updated dependency to: com.lihaoyi::geny:0.7.1
# Updated dependency to: com.lihaoyi::pprint:0.7.3
```
