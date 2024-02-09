---
title: REPL
sidebar_position: 8
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

The `repl` command starts a Scala REPL, which lets you interactively run your code and inspect its results:

<ChainedSnippets>

```bash ignore
scala-cli repl
```

```text
scala> println("Hello Scala")
Hello Scala

scala> :exit
```

</ChainedSnippets>

Scala CLI by default uses the normal Scala REPL.

If you prefer to use the [Ammonite REPL](https://ammonite.io/#Ammonite-REPL), specify `--amm` to launch it rather than the default REPL:

:::caution
Using the Ammonite REPL is restricted and requires setting the `--power` option to be used.
You can pass it explicitly or set it globally by running:

    scala-cli config power true
:::

<ChainedSnippets>

```bash ignore
scala-cli --power repl --amm
```

```text
Loading...
Welcome to the Ammonite Repl 2.4.0-23-76673f7f (Scala 3.0.2 Java 11.0.11)
@ println("Hello ammonite")
Hello ammonite
@ exit
Bye!
```

</ChainedSnippets>

The `repl` command accepts the same arguments as the [compile](compile.md) command. It first compiles any provided sources, and then exposes those results and any provided dependencies to the REPL session:

```scala title=mylibrary/Messages.scala
package mylibrary

object Messages {
  def message = "Hello"
  def print(): Unit = println(message)
}
```

<ChainedSnippets>

```bash ignore
scala-cli repl mylibrary/Messages.scala
```

```text
Compiling project (Scala 3.0.2, JVM)
Compiled project (Scala 3.0.2, JVM)
scala> import mylibrary._

scala> Messages.print()
Hello

scala> :quit
```

</ChainedSnippets>

## Using Toolkit in REPL
It is also possible to start the scala-cli REPL with [toolkit](https://scala-cli.virtuslab.org/docs/guides/introduction/toolkit/) enabled

<ChainedSnippets>
    
```bash ignore
scala-cli repl --toolkit default
```

```text
Welcome to Scala 3.3.1 (17, Java OpenJDK 64-Bit Server VM).
Type in expressions for evaluation. Or try :help.
                                                                                
scala> import os._
                                                                                
scala> os.pwd
val res0: os.Path = /Users/yadukrishnan/test
scala> :quit

```
</ChainedSnippets>

Since we started the repl with toolkit enabled, we can use the libraries included in the toolkit directly. In the above example, the `os-lib` library from the toolkit is used to print the current path. 

## Inject code as JAR file in class path

If your application inspects its class path, and requires only JAR files in it, use `--as-jar` to
put the Scala CLI project in the class path as a JAR file rather than as a directory:

```bash ignore
scala-cli repl Foo.scala --as-jar
```
