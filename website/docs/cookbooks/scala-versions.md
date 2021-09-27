# Picking Scala version with scala-cli

Scala cli by default runs latest stable Scala version.

Here is an unversal piece of code that detect Scala version in runtime

```scala
object ScalaVersion extends App {
    val props = new java.util.Properties
    props.load(getClass.getResourceAsStream("/library.properties"))
    val line = props.getProperty("version.number")
    val Version = """(\d\.\d\.\d).*""".r
    val Version(versionStr) = line
    println(s"Using Scala version: $versions")
}
```

When run without any version provided:

```scala-cli
scala-cli ScalaVersion.scala
```

<!-- Expected-regex:
Using Scala version: 3.*
-->


It will run using latest stable release of Scala (3.0.2 by the time of writting this doc.)

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

will result in picking up a lates stable release for Scala 2 (`2.13.6` as of when this doc is written) and

```scala-cli
scala-cli -S 2.12 ScalaVersion.scala
```
<!-- Expected:
Using Scala version: 2\.12\..*
-->

will use latest stable release of `2.12` `2.12.15`.


We can also pin the version of the language withing the .scala file with `using directives`. You can read our more how using directives works in [documentation](/TODO) or this [examole](/TODO)

::: info
Using directives sytax is still experimental and may change in future versions of scala-cli



:::tip
Not providing a Scala version or proving a partial one (in a form of `2` or `2.12`) is not recomended for projects that requires 