---
title: Basics
sidebar_position: 3
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

Scala CLI is a command line tool that executes a given sub-command on the inputs it’s provided with, using a
given [configuration](../guides/configuration.md) to produce a result.

The most important sub-commands are:

- [compile](./compile.md) compiles your code (excluding tests)
- [run](./run.md) runs your code using the provided arguments (it’s also used when no other command is provided)
- [test](./test.md) compiles and runs the tests defined in your code
- [package](./package.md) packages your code into a jar or other format
- [repl](./repl.md) / [console](./repl.md) runs the interactive Scala shell
- [fmt](./fmt.md) formats your code

Scala CLI can also be run without passing any explicit sub-command,
in which case it defaults to one of the sub-commands based on context:

- if the `--version` option is passed, it prints the `version` command output (unmodified by any other options)
- if any inputs were passed, it defaults to the `run` sub-command
    - and so, `scala-cli a.scala` runs your `a.scala` file
- additionally, when no inputs were passed, it defaults to the `run` sub-command in the following scenarios:
    - if a snippet was passed with `-e`, `--execute-script`, `--execute-scala`, `--execute-java` or `--execute-markdown`
    - if a main class was passed with the `--main-class` option alongside an extra `--classpath`
- otherwise if no inputs were passed, it defaults to the `repl` sub-command

## Input formats

The `scala-cli` CLI commands accept input in a number of ways, most notably:

- as source files
- as one or several directories that contain source files
- as URLs pointing to sources
- by processing source code via piping or process substitution

Note that all of these input formats can be used alongside each other.

## Source files

Scala CLI accepts the following types of source code:

- `.scala` files, containing Scala code
- `.sc` files, containing Scala scripts (see more in the [Scripts guide](../guides/scripts.md))
- `.java` files, containing Java code
- `.md` files, containing Markdown code (experimental, see more in the [Markdown guide](../guides/scripts.md))
- `.c` and `.h` files, containing C code (only as resources for Scala Native, see more in
  the [Scala Native guide](../guides/scala-native.md))
- `.jar` files, (see more in the [Run docs](run#jar))

The following example shows the simplest input format.
First, create a source file:

```scala title=Hello.scala
object Hello {
  def main(args: Array[String]): Unit =
    println("Hello from Scala")
}
```

Then run it by passing it to `scala-cli`:

<ChainedSnippets>

```bash
scala-cli Hello.scala
```

```text
Hello from Scala
```

</ChainedSnippets>

You can also split your code into multiple files:

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

and the run them with `scala-cli`:

<ChainedSnippets>

```bash
scala-cli Hello.scala Messages.scala
```

```text
Hello from Scala
```

</ChainedSnippets>

:::note
Scala CLI compiles only the provided inputs.
For example, if we provide only one of the files above:

```bash fail
scala-cli Hello.scala
```

compilation will fail. `scala-cli` compiles only the files it’s given.
:::

While this is *very* convenient for projects with just a few files, passing many files this way can be cumbersome and
error-prone.
In the case of larger projects, passing whole directories can help.

## Directories

`scala-cli` accepts whole directories as input.

This is convenient when you have many `.scala` files, and passing them all one-by-one on the command line isn't
practical:

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

In this case, you can run all the source code files in `my-app` by supplying the directory name:

<ChainedSnippets>

```bash
scala-cli my-app
```

```text
Hello from Scala
```

</ChainedSnippets>

In our experience, `scala-cli .` is the most used command; it compiles and runs all sources in the current directory.

:::note
Scala CLI process all files within the specified directories and all of its subdirectories.

Scala CLI ignores all subdirectories that start with `.` like `.scala-build` or `.vscode`.
Such directories needs to be explicitly provided as inputs.
:::

## URLs

:::warning
Running unverified code from the internet can be very handy for *trusted* sources, but it can also be really dangerous,
since Scala CLI does not provide any sandboxing at this moment.

Make sure that you trust the code that you are about to run.
:::

`scala-cli` accepts input via URLs pointing at `.scala` files.
It downloads their content, and runs them:

<ChainedSnippet>

```bash
scala-cli https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.scala
```

```text
Hello from Scala GitHub Gist
```

</ChainedSnippet>

### GitHub Gist

`scala-cli` accepts input via Github Gist’s urls.
It downloads the gist zip archive and runs it:

<ChainedSnippets>

```bash
scala-cli https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec
```

```text
Hello
```

</ChainedSnippets>

More details in the [GitHub gists cookbook](../cookbooks/gists.md).

### Zip archive

`scala-cli` accepts inputs via a `zip` archive path.
It unpacks the archive and runs it:

```scala titleHello.scala
object Hello extends App {
  println("Hello")
}
```

<ChainedSnippets>

```bash ignore
unzip -l hello.zip
```

```text
Archive:  hello.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
       49  12-07-2021 00:06   Hello.scala
---------                     -------
       49                     1 file
```

```bash ignore
scala-cli hello.zip
```

```text
Hello
```

</ChainedSnippets>

## Piping

You can also pipe code to `scala-cli` for execution:

- scripts

  <ChainedSnippets>

  ```bash
  echo 'println("Hello")' | scala-cli _.sc
  ```
  
  ```text
  Hello
  ```

  </ChainedSnippets>

- Scala code

  <ChainedSnippets>

  ```bash
  echo '@main def hello() = println("Hello")' | scala-cli _.scala
  ```
  
  ```text
  Hello
  ```

  </ChainedSnippets>

- Java code

  <ChainedSnippets>

  ```bash
  echo 'class Hello { public static void main(String args[]) { System.out.println("Hello"); } }' | scala-cli _.java
  ```
  
  ```text
  Hello
  ```

  </ChainedSnippets>

- Markdown code (experimental)

  <ChainedSnippets>

  ```bash
  echo '# Example Snippet
  ```scala
  println("Hello")
  ```' | scala-cli _.md
  ```
  
  ```text
  Hello
  ```

  </ChainedSnippets>

More details in the [Piping guide](../guides/piping.md).

## Scala CLI version

`scala-cli` can also run another Scala CLI version, which can be helpful to test unreleased Scala CLI functionalities.
:::warning
Running another Scala CLI version might be slower because it uses JVM-based Scala CLI launcher.
:::

To run another Scala CLI version, specify it with `--cli-version` before any other argument:

<ChainedSnippets>

```bash
scala-cli --cli-version 0.1.17-62-g21e1cf44-SNAPSHOT about
```

```text
Scala CLI version: 0.1.17-62-g21e1cf44-SNAPSHOT
Scala version (default): 3.2.1
```

</ChainedSnippets>

<!-- Expected:
Scala CLI version: 0.1.17-62-g21e1cf44-SNAPSHOT
Scala version (default): 3.2.1
-->

To use the latest Scala CLI nightly build, pass `nightly` to `--cli-version` parameter:

<ChainedSnippets>

```bash
scala-cli --cli-version nightly about
```

```text
Fetching Scala CLI 0.1.17-62-g21e1cf44-SNAPSHOT
Scala CLI version: 0.1.17-62-g21e1cf44-SNAPSHOT
Scala version (default): 3.2.1
```

</ChainedSnippets>

## Process substitution

Lastly, `scala-cli` also accepts input via shell process substitution:

<ChainedSnippets>

```bash
scala-cli <(echo 'println("Hello")')
```

```text
Hello
```

</ChainedSnippets>

<!-- Expected:
Hello
-->
