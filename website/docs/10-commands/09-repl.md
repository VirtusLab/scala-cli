---
title: REPL
---

The `repl` command starts a Scala REPL, that allows you to interactively
run your code and inspect its results.

```bash ignore
scala-cli repl
# scala> println("Hello Scala")
# Hello Scala
#
# scala> :exit
```

Scala CLI by default uses REPL shiped together with scala compiler.

Pass `--amm` to launch an [Ammonite REPL](https://ammonite.io/#Ammonite-REPL), rather than the default Scala REPL:

```bash ignore
scala-cli repl --amm
# Loading...
# Welcome to the Ammonite Repl 2.4.0-23-76673f7f (Scala 3.0.2 Java 11.0.11)
# @ println("Hello ammonite") 
# Hello ammonite
# @ exit 
# Bye!
```

`repl` commands accepts same arguments like [compile](/docs/guides/compile) commands. It first compile provided sources and then exposes results as well as provided dependencies to the REPL session:

```scala title=mylibrary/Messages.scala
package mylibrary

object Messages {
  def message = "Hello"
  def print(): Unit = println(message)
}
```
```bash
scala-cli repl mylibrary/Messages.scala
# Compiling project (Scala 3.0.2, JVM)
# Compiled project (Scala 3.0.2, JVM)
# scala> import mylibrary._
#
# scala> Messages.print()
# Hello
#
# scala> :quit
```
