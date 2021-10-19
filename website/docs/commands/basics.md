---
title: Basics
sidebar_position: 3
---

Scala CLI is a command line tools that execute given command on provided inputs with given [configuration](../guides/configuration.md) to produce a result. Most important commands are:

  - [compile](./compile.md) to compile you code (this exclude tests)
  - [run](./run.md) - to run your code using provided arguments (also used when no other command is provided)
  - [test](./test.md) - to compile and run tests defined in your code
  - [package](./package.md) - to package your code into a jar or other format
  - [repl](./repl.md) / [console](./repl.md) - to run interactive Scala shell
  - [fmt](./fmt.md) - to format your code

Scala CLI can be run without any command provided and that will default to the `run` command, so `scala-cli a.scala` will run your a.scala file.

## Input formats

The `scala-cli` CLI commands accept input in a number of ways, most notably:
- as source files
- as one or several directories, containing sources
- as URLs, pointing to sources
- by piping or process substitution source code directly

Lastly, note that all these input formats can used alongside each other.

## Source files

Scala CLI accepts following kinds of source:
 - `.scala` files containing Scala code
 - `.sc` files, containing Scala scripts (see more in [Scripts guide](../guides/scripts.md))
 - `.java` files containing Java code

This is the simplest input format. Just write a source file, and pass it to
`scala-cli` to run it:

```scala title=Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello from Scala")
}
```

Run it with
```bash
scala-cli Hello.scala
# Hello from Scala
```

You can also split your code in multiple files, and pass all of them to `scala-cli` :

```scala title=Messages.scala
object Messages {
  def hello = "Hello from Scala"
}
```

```scala title=Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println(Messages.hello)
}
```

Run them with
```bash
scala-cli Hello.scala Messages.scala
# Hello from Scala
``` 

:::note
Scala CLI compiles together only the provided inputs.
:::

If we provide only one of the files above:

```bash fail
scala-cli Hello.scala
```

compilation will fail even though a moment ago files compiled together without any problem.

Passing many files this way can be cumbersome and error-prone. Directories can help.

## Directories

`scala-cli` accepts whole directories as input. This is convenient when you have many
`.scala` files, and passing them all one-by-one on the command line isn't practical:

```scala title=my-app/Messages.scala
object Messages {
  def hello = "Hello from Scala"
}
```

```scala title=my-app/Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println(Messages.hello)
}
```

Run them with
```bash
scala-cli my-app
# Hello from Scala
```

From our experience, `scala-cli .` is the most used command (it will compile and run all sources from within current directory.)

:::note
Scala CLI will process all files within the directories and all its subdirectories.

Scala CLI ignores all subdirectories that starts with `.` like `.scala` or `.vscode`. Such directories needs to be explicitly provided as inputs.
:::
## URLs

:::warning
Running unverified code from the internet may be really dangerous since Scala CLI does not provide any sandboxing at this moment.

Make sure that you trust the code that you are about to run.
:::

`scala-cli` accepts input via URLs pointing at `.scala` files.
It will download their content, and run them.

```bash
scala-cli https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.scala
# Hello from Scala GitHub Gist
```

### GitHub Gist

`scala-cli` accepts input via Github Gists url.
It'll download gists zip archive, cache their content, and run them.

```bash
scala-cli https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec
# Hello
```

## Piping

You can just pipe Scala code to `scala-cli` for execution:
```bash
echo 'println("Hello")' | scala-cli -
# Hello
```

## Process substitution

`scala-cli` accepts input via shell process substitution:
```bash
scala-cli <(echo 'println("Hello")')
# Hello
```lo")')
# Hello
```