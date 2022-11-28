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

<ChainedSnippets>

```bash ignore
scala-cli repl --amm
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

The `repl` command accepts the same arguments as the [compile](./compile.md) command. It first compiles any provided sources, and then exposes those results and any provided dependencies to the REPL session:

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
