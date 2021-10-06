---
title: Picking Scala version with scala-cli
sidebar_position: 2
---

# Picking Scala version with scala-cli

Scala cli by default runs latest stable Scala version.

Here is an universal piece of code that detect Scala version in runtime

```scala name:ScalaVersion.scala
object ScalaVersion extends App {
    val props = new java.util.Properties
    props.load(getClass.getResourceAsStream("/library.properties"))
    val line = props.getProperty("version.number")
    val Version = """(\d\.\d+\.\d+).*""".r
    val Version(versionStr) = line
    println(s"Using Scala version: $versionStr")
}
```

When run without any version provided:

```scala-cli
scala-cli ScalaVersion.scala
```

<!-- Expected-regex:
Using Scala version: 2.*
-->


It will run using latest stable release of Scala (3.0.2 by the time of writing this doc.)

Scala version can be also provided from command line using `--scala` (with `-S` and `--scala-version` aliases)

```scala-cli
scala-cli -S 2.13.5 ScalaVersion.scala
```
<!-- Expected:
Using Scala version: 2.13.5
-->

In most cases we do not care for a precise Scala version and 'any Scala 2' or `2.13` is good enough for us. 

Scala cli accepts version prefixes so:

```scala-cli
scala-cli -S 2 ScalaVersion.scala
```
<!-- Expected:
Using Scala version: 2/.*
-->

will result in picking up a latest stable release for Scala 2 (`2.13.6` as of when this doc is written) and

```scala-cli
scala-cli -S 2.12 ScalaVersion.scala
```
<!-- Expected:
Using Scala version: 2\.12\..*
-->

will use latest stable release of `2.12` `2.12.15`.


We can also pin the version of the language within the .scala file with `using directives`. You can read our more how using directives works in documentation and examples.

:::info
Using directives syntax is still experimental and may change in future versions of scala-cli


So when we will have:

```scala name:version.scala
// using scala 2.12.5

//rest of the config
```

and run

```scala-cli
scala-cli ScalaVersion.scala version.scala
```

We will reulst in using `2.12.5`. 

scala-cli is command-line first so any configuration pass to command line will override using directives.

So, running 

```scala-cli
scala-cli -S 2.13.5 ScalaVersion.scala version.scala
```

Will result in using `2.13.5`

<!-- Expected:
Using Scala version: 2\.12\..*
-->

# When should I provide a full version of scala?

For prototyping, scripting and other use cases that does not require to run code multiple times in the future proving version is not required. 

Scala is source and binary compatible within each major version (e.g. `2.12.x` or `3.1.x`) so providing version in `eopch.major` form (e.g. `2.12`, `2.13` or `3.1`) should be perfectly fine for most use cases. When your Scala code contains more advanced features that may be more sensitive for changes in minor version (e.g. from `2.13.4` to `2.13.5`) we recommend using complete Scala version.
