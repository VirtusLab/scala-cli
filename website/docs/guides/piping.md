---
title: Piping
sidebar_position: 42
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

# Piping

Instead of passing paths to your sources, you can also pipe your code via standard input:

<ChainedSnippets>

```bash
echo '@main def hello() = println("Hello")' | scala-cli _
```

```text
Hello
```

</ChainedSnippets>

## Wildcards

The `_` wildcard implies that the piped code is a standard Scala app.
It is also possible to pass a script or Java code, when using the appropriate wildcard.
The available options are as follows:

- for standard Scala code use `_`, `_.scala` or `-.scala`;
- for Scala scripts use `-`, `_.sc` or `-.sc`;
- for Java code use `_.java` or `-.java`;
- for Markdown code use `_.md` or `-.md`.

## Examples

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

## Mixing piped sources with on-disk ones

It is also possible to pipe some code via standard input, while the rest of your code is on-disk.

<ChainedSnippets>

```bash
echo 'case class HelloMessage(msg: String)' > HelloMessage.scala
echo '@main def hello() = println(HelloMessage(msg = "Hello").msg)' | scala-cli _ HelloMessage.scala
```

```text
Hello
```

</ChainedSnippets>

You can even refer to code from piped scripts, when needed. A piped script can be referred to by its wrapper
name `stdin`, as in the example below.

<ChainedSnippets>

```bash
echo '@main def main() = println(stdin.message)' > PrintMessage.scala
echo 'def message: String = "Hello"' | scala-cli PrintMessage.scala _.sc
```

```text
Hello
```

</ChainedSnippets>
