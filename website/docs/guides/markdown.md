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

## Markdown inputs

### On-disk markdown sources

You can pass local `.md` inputs by passing their path to Scala CLI (as you would for any other kind of input).

```bash ignore
scala-cli hello.md
```

`.md` sources inside of directories are ignored by default, unless the `--enable-markdown` option is passed.

```bash ignore
scala-cli dir-with-markdown --enable-markdown
```

### Zipped archives

Scala CLI can run `.md` sources inside a `.zip` archive.
Same as with directories,  `.md` sources inside zipped archives are ignored by default, unless
the `--enable-markdown` option is passed.

```bash ignore
scala-cli archive-with-markdown.zip --enable-markdown
```

### Remote inputs

:::warning
Running unverified code from the Internet can be very handy for *trusted* sources, but it can also be really dangerous,
since Scala CLI does not provide any sandboxing at this moment.

Make sure that you trust the code that you are about to run.
:::

#### URLs

You can also pass a URL pointing to a `.md` file to run it with Scala CLI.

<ChainedSnippets>

```bash
scala-cli https://gist.githubusercontent.com/Gedochao/6415211eeb8ca4d8d6db123f83f0f839/raw/4c5ce7593e19f1390555221e0d076f4b02f4b4fd/example.md
```

```text
Hello
```

</ChainedSnippets>

#### Github Gist

Scala CLI accepts GitHub Gist URLs.
The gist is technically treated as a zipped archive (which it is downloaded as), so it is necessary to pass
the `--enable-markdown` option alongside the gist URL to run any contained Markdown sources.

<ChainedSnippets>

```bash
scala-cli https://gist.github.com/Gedochao/6415211eeb8ca4d8d6db123f83f0f839 --enable-markdown
```

```text
Hello
```

</ChainedSnippets>

You can find more information on running GitHub Gists in the [gists cookbook](../cookbooks/gists.md).

### Piped Markdown code

Instead of passing paths to your Markdown sources, you can also pipe your code via standard input:

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

You can find more information on piped sources in the [piping guide](./piping.md).

### Markdown code as a command line snippet

It is also possible to pass Markdown code as a snippet directly from the command line.

<ChainedSnippets>

````bash
scala-cli run --markdown-snippet '# Markdown snippet
with a code block
```scala
println("Hello")
```'
````

```text
Hello
```

</ChainedSnippets>

You can find more information on command line snippets in the [snippets guide](./snippets.md).

## Markdown code blocks

### Plain `scala` snippets

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

### `scala raw` snippets

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

### `scala test` snippets

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

### `reset` scope for `scala` snippets

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

## `using` directives and markdown code blocks

It is possible to define `using` directives at the beginning of a `scala` code block inside a markdown input.
This is supported for all `scala` code block flavours.

````markdown title=UsingDirectives.md
# Using directives in `.md` inputs

## `scala raw` example
```scala raw
//> using lib "com.lihaoyi::pprint:0.8.0"
object Printer {
  def printHello(): Unit = pprint.pprintln("Hello")
}
```

## Plain `scala` example
```scala
//> using lib "com.lihaoyi::os-lib:0.8.1"
println(os.pwd)
```

## `scala test` example
```scala test
//> using lib "org.scalameta::munit:0.7.29"

class Test extends munit.FunSuite {
  test("foo") {
    assert(true)
    println("Hello from tests")
  }
}
```
## Relying on directives from other snippets
Directives from other snippets apply to the whole context.
As a result, nothing really stops you from using a dependency
from an earlier code block.
```scala
Printer.printHello()
pprint.pprintln("world")
```
````

:::note
`scala` snippets inside of a Markdown input are not isolated. Each `using` directive applies to the whole project's
context. A directive defined in a later snippet within the same source may override another defined in an earlier one.

````markdown title="OverriddenDirective.md"
## 1

```scala
//> using scala "2.12.17"
println(util.Properties.versionNumberString)
```

## 2

```scala
//> using scala "2.13.10"
println(util.Properties.versionNumberString)
```
````

In this example, the directive from the second `scala` snippet will override the previous one and Scala `2.13.10` will
be used for both.

<ChainedSnippets>

```bash ignore
scala-cli OverriddenDirective.md
```

```text
Compiling project (Scala 2.13.10, JVM)
Compiled project (Scala 2.13.10, JVM)
2.13.10
2.13.10
```

</ChainedSnippets>

:::

## Referring to code from Markdown

### Plain `scala` code blocks

Referring to code from plain `scala` snippets in markdown requires using their package name.
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
