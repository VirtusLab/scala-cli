---
title: Doc
sidebar_position: 18
---

Scala CLI can generate the API documentation of your Scala 2, Scala 3, and Java projects. It provides features similar
to `javadoc`.
The API documentation is generated in a directory whose files make up a static website:

```scala title=Hello.scala
package hello

/** Hello object for running main method
 */
object Hello {
  /**
   * Main method
   * @param args The command line arguments.
   * */
  def main(args: Array[String]): Unit =
    println("Hello")
}
```

```bash
scala-cli doc Hello.scala -o scala-doc
# Wrote Scaladoc to ./scala-doc
```

<!-- Expected
Wrote Scaladoc to ./scala-doc
-->

The output directory `scala-doc` contains the static site files with your documentation.

## Cross-building documentation ⚡️

:::caution
The `--cross` option is experimental and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true

:::

Use `--cross` (with `--power`) to build and generate Scaladoc for **every** Scala version and platform combination
configured for your project—the same behavior as `run` and `package` with `--cross`. This is useful when you have
multiple Scala versions or platforms and want documentation for each.

Example: a library that supports both Scala 2.13 and 3.3 LTS:

```scala title=Example.scala
//> using scala 2.13 3.3.7
package lib

/** Example class for cross-built documentation. */
class Example {
  /** Returns a greeting. */
  def greet: String = "Hello"
}
```

When `--cross` produces multiple cross builds, the output directory is split into one subdirectory per combination: by
default a subdirectory per Scala version (e.g. `doc-out/2.13.18`, `doc-out/3.3.7`), and when targeting multiple
platforms, each subdirectory name includes the platform (e.g. `doc-out/3.3.7_jvm`). This avoids overwriting docs from
different builds.

```bash
scala-cli --power doc --cross . -o doc-out
# Wrote Scaladoc to doc-out/2.13.18
# Wrote Scaladoc to doc-out/3.3.7
```

Without `--cross`, only a single build (the default Scala version and platform) is documented and written to the given
output path.

After opening the generated static documentation (you have to open `scala-doc/index.html` in your browser),
you will see the generated scaladoc documentation. The following screen shows the definition of the `main` method:

import ScalaDocMainMethod from '@site/static/img/scala-doc-main-method.png';

<img src={ScalaDocMainMethod} />