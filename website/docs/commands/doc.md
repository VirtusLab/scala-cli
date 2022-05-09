---
title: Doc
sidebar_position: 17
---

Scala CLI can generate the API documentation of your Scala 2 and 3 projects. It provides similar features to `javadoc`.
 Scaladoc documentation is generated in directory with static site files:


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

Output directory `scala-doc` contains static site files with your documentation.

After opening generated static documentation (you have to open `scala-doc/index.html` in your browser), 
you will see generated Scaladoc documentation. The following screen shows the definition `main` method:

import ScalaDocMainMethod from '@site/static/img/scala-doc-main-method.png';

<img src={ScalaDocMainMethod} />