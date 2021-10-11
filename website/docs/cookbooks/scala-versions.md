---
title: Picking Scala version with scala-cli
sidebar_position: 2
---

# Picking Scala version with scala-cli

Scala cli by default runs latest stable Scala version.

Here is an universal piece of code that detect Scala version in runtime. Code is a bit complicated so we suggest to skip reading the whole file and just focus on what it prints.

```scala title=ScalaVersion.scala
object ScalaVersion extends App {
  def props(url: java.net.URL): java.util.Properties = {
    val properties = new java.util.Properties()
    val is = url.openStream()
    try {
      properties.load(is)
      properties
    } finally is.close()    
  }

  def scala2Version: String = 
    props(getClass.getResource("/library.properties")).getProperty("version.number")
    
  def checkScala3(res: java.util.Enumeration[java.net.URL]): String = 
    if (!res.hasMoreElements) scala2Version else {
      val manifest = props(res.nextElement)
      manifest.getProperty("Specification-Title") match {
        case "scala3-library-bootstrapped" =>
          manifest.getProperty("Implementation-Version")
        case _ => checkScala3(res)
      }
    }
  val manifests = getClass.getClassLoader.getResources("META-INF/MANIFEST.MF")
    
  val scalaVersion = checkScala3(manifests)
  val javaVersion = System.getProperty("java.version")

  println(s"Scala: $scalaVersion")
}
```

When run without any version provided:

```bash
scala-cli ScalaVersion.scala
```

<!-- Expected-regex:
Scala: 3\..*
-->


It will run using latest stable release of Scala (3.0.2 by the time of writing this doc.)

Scala version can be also provided from command line using `--scala` (with `-S` and `--scala-version` aliases)

```bash
scala-cli -S 2.13.5 ScalaVersion.scala
```
<!-- Expected-regex:
Scala: 2\.13\.5
-->

In most cases we do not care for a precise Scala version and 'any Scala 2' or `2.13` is good enough for us. 

Scala cli accepts version prefixes so:

```bash
scala-cli -S 2 ScalaVersion.scala
```
<!-- Expected-regex:
Scala: 2\..+
-->

will result in picking up a latest stable release for Scala 2 (`2.13.6` as of when this doc is written) and

```bash
scala-cli -S 2.12 ScalaVersion.scala
```
<!-- Expected-regex:
Scala: 2\.12\.15
-->

will use latest stable release of `2.12` `2.12.15`.


We can also pin the version of the language within the .scala file with `using directives`. You can read our more how using directives works in documentation and examples.

:::info
Using directives syntax is still experimental and may change in future versions of scala-cli
:::

So when we will have:

```scala title=version.scala
// using scala 2.12.5

object OldCode
//rest of the config
```

and run

```bash
scala-cli ScalaVersion.scala version.scala
```

<!-- Expected-regex: TODO - 
Scala: 2\.12\.5
-->

We will results in using `2.12.5`. 

scala-cli is command-line first so any configuration pass to command line will override using directives.

So, running 

```bash
scala-cli -S 2.13.5 ScalaVersion.scala version.scala
```

Will result in using `2.13.5`

<!-- Expected-regex:
Scala: 2\.13\.5
-->

## When should I provide a full version of scala?

For prototyping, scripting and other use cases that does not require to run code multiple times in the future proving version is not required. 

Scala is source and binary compatible within each major version (e.g. `2.12.x` or `3.1.x`) so providing version in `eopch.major` form (e.g. `2.12`, `2.13` or `3.1`) should be perfectly fine for most use cases. When your Scala code contains more advanced features that may be more sensitive for changes in minor version (e.g. from `2.13.4` to `2.13.5`) we recommend using complete Scala version.
