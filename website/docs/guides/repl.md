---
title: REPL
sidebar_position: 9
---

The `repl` command starts a Scala REPL, that allows you to interactively
run your code and inspect its results.

```bash
cat Messages.scala
# package mylibrary
# object Messages {
#   def message = "Hello"
#   def print(): Unit = println(message)
# }

scala-cli repl Messages.scalascala-cli repl Messages.scala
# Compiling project (Scala 3.0.2, JVM)
# Compiled project (Scala 3.0.2, JVM)
# scala> import mylibrary._
#
# scala> Messages.print()
# Hello
#
# scala> :quit
```

Pass `--amm` to launch an Ammonite REPL, rather than the default Scala REPL:
```bash
scala-cli repl Messages.scala --amm
# Compiling project (Scala 3.0.2, JVM)
# Compiled project (Scala 3.0.2, JVM)
# Loading...
# Welcome to the Ammonite Repl 2.4.0-23-76673f7f (Scala 3.0.2 Java 11.0.7)
# @ import mylibrary._
# import mylibrary._
#
# @ Messages.print()
# Hello
#
# @ exit
# Bye!
```
