---
title: Doc
sidebar_position: 18
---

Scala CLI can generate the API documentation of your Scala 2, Scala 3, and Java projects. It provides features similar to `javadoc`.
The API documentation is generated in a directory whose files make up a static website:

```scala title=Hello.scala
package hello
/** Hello object for running main method
 */
object Hello {
  /**
    * Main method
    * @param args The command line arguments.
    **/
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

After opening the generated static documentation (you have to open `scala-doc/index.html` in your browser),
you will see the generated scaladoc documentation. The following screen shows the definition of the `main` method:

import ScalaDocMainMethod from '@site/static/img/scala-doc-main-method.png';

<img src={ScalaDocMainMethod} />