---
title: Picking the Scala version with Scala CLI
sidebar_position: 2
---

By default, Scala CLI runs the latest supported scala version. See our list of [Supported Scala Versions](/docs/reference/scala-versions) in Scala CLI.

To demonstrate how this works, here’s a universal piece of code that detects the Scala version at runtime.
The code is a bit complicated, so we suggest that you skip reading the whole file, and just focus on what it prints:

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

When this application is run without specifying a Scala version, it uses the latest stable release of Scala — 3.1.0 at the time of writing this doc:

```bash
scala-cli ScalaVersion.scala
```

<!-- Expected-regex:
Scala: 3\..*
-->

When you want to control the Scala version, you can control it from the command line using the `--scala` option (with `-S` and `--scala-version` aliases):

```bash
scala-cli -S 2.13.5 ScalaVersion.scala
```
<!-- Expected-regex:
Scala: 2\.13\.5
-->

In many cases you won't care for a precise Scala version and will want "any Scala 2" or "any 2.13 release."
For this situation, Scala CLI accepts version prefixes like this:

```bash
scala-cli -S 2 ScalaVersion.scala
```
<!-- Expected-regex:
Scala: 2\..+
-->

and this:

```bash
scala-cli -S 2.12 ScalaVersion.scala
```
<!-- Expected-regex:
Scala: 2\.12\.17
-->

In the first example (`-S 2`), the application picks up the latest Scala 2 stable release (`2.13.9` at the time of this writing).
In the second example, the application picks up the latest stable release of `2.12` (which is `2.12.17` at the time of this writing).

You can also pin the version of the language within a `.scala` file with `using` directives.

:::info
The `using` directives syntax is still experimental, and may change in future versions of Scala CLI.
:::

Here’s an example of a source code file named `version.scala` that contains a `using` directive:

```scala title=version.scala
//> using scala "2.12"

object OldCode
//rest of the config
```

Now when you compile that code along with the previous `ScalaVersion.scala` file:

```bash
scala-cli ScalaVersion.scala version.scala
```

<!-- Expected-regex:
Scala: 2\.12\.17
-->

The output at the time of this writing is "`2.12.17`".

The Scala CLI philosophy is “command line first,” so any configuration information that’s passed to the command line will override `using` directives. So when you run this command with the `-S` option:

```bash
scala-cli -S 2.13.10 ScalaVersion.scala version.scala
```

the result is "`2.13.10`" (as opposed to "`2.12.17`" in the previous example).

<!-- Expected-regex:
Scala: 2\.13\.10
-->

:::note
See our [Using Directives Guide](/docs/guides/using-directives.md) for more details on `using` directives.
:::


## When should I provide a full version of Scala?

For prototyping, scripting, and other use cases that won’t need to be run multiple times in the future, providing a Scala version generally isn’t necessary.

Scala is source and binary compatible within each major version (e.g., `2.12.x` or `3.1.x`) so specifying the version in `epoch.major` form (e.g., `2.12`, `2.13`, or `3.1`) should be perfectly fine for most use cases. When your Scala code contains more advanced features that may be more sensitive for changes in minor version (e.g., from `2.13.4` to `2.13.5`) we recommend specifying the complete Scala version.
