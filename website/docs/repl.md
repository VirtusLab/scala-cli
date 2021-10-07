---
title: REPL
sidebar_position: 9
---

The `repl` command starts a Scala REPL, that allows you to interactively
run your code and inspect its results.

It uses on the [Ammonite](http://ammonite.io) REPL, which is more featureful
and user-friendly than the REPL that comes up with the Scala compiler.

```bash
cat Messages.scala
# package mylibrary
# object Messages {
#   def message = "Hello"
#   def print(): Unit = println(message)
# }

scala-cli repl Messages.scala
# Loading...
# Welcome to the Ammonite Repl 2.4.0 (Scala 2.12.13 Java 11.0.7)
# @ import mylibrary._
# import mylibrary._
# @ Messages.print()
# Hello
# @ exit
```
