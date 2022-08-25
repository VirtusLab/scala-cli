---
title: Markdown (experimental)
sidebar_position: 21
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

Scala CLI can compile, run, test, and package markdown (`.md`) sources.

:::note
This feature is a work in progress and should currently be treated as experimental.
Markdown sources are ignored by default unless passed explicitly as inputs.
You can enable including non-explicit `.md` inputs by passing the `--enable-markdown` option.
:::

## Plain `scala` snippets

````markdown title=Example.md
# Example

This is a simple example of an `.md` file with a Scala snippet.

```scala
val message = "Hello from Markdown"
println(message)
```
````

Plain `scala` snippets are treated similarly to `.sc` scripts in that any kind of statement is accepted at the
top-level.

<ChainedSnippets>

```bash ignore
scala-cli run Example.md
```

```text
Hello from Markdown
```

</ChainedSnippets>

Similarly to `.sc` scripts, when multiple `.md` files with plain `scala` snippets are being run, each of them will have
its own main class, that can be run.

<ChainedSnippets>

```bash ignore
scala-cli Example1.md Example2.md
```

```text
[error]  Found several main classes: Example1_md, Example2_md
```

</ChainedSnippets>

When multiple such sources are passed as inputs, the main class has to be passed explicitly with the `--main-class`
option.

<ChainedSnippets>

```bash ignore
scala-cli Example1.md Example2.md --main-class Example1_md
```

```text
Hello from Markdown
```

</ChainedSnippets>

You can always check what main classes are available in the context with the `--list-main-classes` option.

<ChainedSnippets>

```bash ignore
scala-cli Example1.md Example2.md --list-main-classes
```

```text
Example1_md Example2_md
```

</ChainedSnippets>

## `scala raw` snippets

You can mark a `scala` code block with the `raw` keyword, indicating that this snippet should not be wrapped as a script
and should instead be treated as is. This is the equivalent of code in a `.scala` file. For a `raw` snippet to be
runnable a main class has to be included.

````markdown title=RawExample.md
# `raw` example

This is a simple example of an `.md` file with a raw Scala snippet.

```scala raw
object Main extends App {
  val message = "Hello from Markdown"
  println(message) 
}
```
````

<ChainedSnippets>

```bash ignore
scala-cli RawExample.md
```

```text
Hello from Markdown
```

</ChainedSnippets>

## `scala test` snippets

It is possible to run tests from `scala` code blocks marked as `test`. This is similar to `raw` snippets in that the
code is not wrapped and is treated as is.

You can run `scala test` code blocks with the `test` sub-command.

````markdown title=TestExample.md
# `test` example
This is a simple example of an `.md` file with a test Scala snippet.

```scala test
//> using lib "org.scalameta::munit:0.7.29"
class Test extends munit.FunSuite {
  test("example test") {
    assert(true)
  }
}
```
````

<ChainedSnippets>

```bash ignore
scala-cli test TestExample.md
```

```text
Test:
  + example test
```

</ChainedSnippets>

## `reset` scope for `scala` snippets

When multiple plain `scala` snippets are used in a single `.md` file, by default they are actually treated as a single
script. They share context and when run, are executed one after another, as if they were all in a single `.sc` file.

If you want a snippet to use a fresh context instead, you can rely on the `reset` keyword. This allows you to start a
fresh scope for the marked snippet (and any coming after it).

````markdown title=ResetExample.md
# `reset` scope
This is an example of an `.md` file with multiple `scala` snippets with separate scopes

## Scope 1
```scala
val message = "Hello"
```

## Still scope 1, since `reset` wasn't used yet
```scala
println(message)
```

## Scope 2
```scala reset
val message = "world"
println(message)
```

## Scope 3
```scala reset
val message = "!"
println(message)
```
````

<ChainedSnippets>

```bash ignore
scala-cli test ResetExample.md
```

```text
Hello
world
!
```

</ChainedSnippets>

## Referring to code from Markdown

### Plain `scala` code blocks

Referring to code from plain `scala` snippets in Markdown requires using their package name.
Similarly to scripts, the package is inferred based on the relative path to the source file in your project.

You also have to point to the Scope under which the code is located.
Scopes are numbered according to their order in a given `.md` file (starting from 0 for the first plain `scala`
snippet): `Scope{scopeNumber}`. The `snippetNumber` is omitted for the first script code block (0). In other words,
the first scope is just `Scope`, the second is `Scope1`, then `Scope2` and so on.

````markdown title=src/markdown/Example.md
## Scope 1
```scala
def hello: String = "Hello"
```

## Still scope 1, since `reset` wasn't used yet
```scala
def space: String = " "
```

## Scope 2
```scala reset
def world: String = "world"
```
````

```scala title=Main.scala
object Main extends App {
  val hello = src.markdown.Example_md.Scope.hello
  val space = src.markdown.Example_md.Scope.space
  val world = src.markdown.Example_md.Scope.world
  println(s"$hello$space$world)
}
```

<ChainedSnippets>

```bash ignore
scala-cli . --enable-markdown --main-class Main
```

```text
Hello world
```

</ChainedSnippets>

### `scala raw` and `scala test` code blocks

You can refer to code from `scala raw` and `scala test` snippets as if they were the contents of a `.scala` file.

````markdown title=RawSnippetToReferTo.md
# `raw` snippet
```scala raw
object Something {
  def message: String = "Hello"
}
```
````

<ChainedSnippets>

```bash ignore
scala-cli RawSnippetToReferTo.md -e 'println(Something.message)'
```

```text
Hello
```

</ChainedSnippets>
