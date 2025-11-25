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

## Passing REPL options
It is also possible to manually pass REPL-specific options.
It can be done in a couple ways:
- after the `--` separator, as the REPL itself is the launched app, so its options are app arguments

<ChainedSnippets>

```bash ignore
scala repl -S 3.6.4-RC1 -- --repl-init-script 'println("Hello")'
```

```
Hello
Welcome to Scala 3.6.4-RC1 (23.0.1, Java OpenJDK 64-Bit Server VM).
Type in expressions for evaluation. Or try :help.
                                                                                                                 
scala> 
```
</ChainedSnippets>


- with the `-O`, effectively passing them as compiler options:

<ChainedSnippets>

```bash ignore
scala repl -S 3.6.4-RC1 -O --repl-init-script -O 'println("Hello")'
```

```
Hello
Welcome to Scala 3.6.4-RC1 (23.0.1, Java OpenJDK 64-Bit Server VM).
Type in expressions for evaluation. Or try :help.
                                                                                                                 
scala> 
```

</ChainedSnippets>

- via a using directive, treating them as Scala compiler options:

<ChainedSnippets>

```scala compile title=repl-options.scala
//> using toolkit default
//> using options --repl-init-script "import os.*; println(pwd)"
```

```bash ignore
scala repl repl-options.scala
```

```
/current/directory/path
Welcome to Scala 3.7.4 (17, Java OpenJDK 64-Bit Server VM).
Type in expressions for evaluation. Or try :help.
                                                                         
scala> pwd
val res0: os.Path = /current/directory/path
scala> :quit
```

</ChainedSnippets>

- directly, as a Scala CLI option (do note that newly added options from an RC version or a snapshot may not be supported this way just yet):

<ChainedSnippets>

```bash ignore
scala repl --repl-init-script 'println("Hello")'
```

```
Hello
Welcome to Scala 3.6.4-RC1 (23.0.1, Java OpenJDK 64-Bit Server VM).
Type in expressions for evaluation. Or try :help.
                                                                         
scala> pwd
val res0: os.Path = /current/directory/path
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
Welcome to Scala 3.7.4 (17, Java OpenJDK 64-Bit Server VM).
Type in expressions for evaluation. Or try :help.
                                                                                
scala> import os.*
                                                                                
scala> os.pwd
val res0: os.Path = /current/directory/path
scala> :quit

```
</ChainedSnippets>

Since we started the repl with toolkit enabled, we can use the libraries included in the toolkit directly. In the above example, the `os-lib` library from the toolkit is used to print the current path. 

### Running snippets directly via the REPL
It is possible to run code snippets via the REPL, with all the internal quirks of the REPL, rather than using standard runner execution.
While the difference is subtle and should generally be invisible to users, it is useful for investigating REPL behavior.
The key to doing that is to use the both `--repl-init-script` and `--repl-quit-after-init` options together.

<ChainedSnippets>


```bash
scala repl --repl-init-script 'println(42)' --repl-quit-after-init # runs in the REPL and quits immediately
```

```
42
```

```bash
scala repl -e '//> using options --repl-init-script "println(42)" --repl-quit-after-init' # same, but via using directive
```

```
42
```

```bash
scala -e 'println(42)' # standard runner equivalent
```

```
42
```

</ChainedSnippets>

## Inject code as JAR file in class path

If your application inspects its class path, and requires only JAR files in it, use `--as-jar` to
put the Scala CLI project in the class path as a JAR file rather than as a directory:

```bash ignore
scala-cli repl Foo.scala --as-jar
```
