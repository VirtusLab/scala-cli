---
title: Scala Toolkit
sidebar_position: 7
---

import {ChainedSnippets} from "../../../src/components/MarkdownComponents.js";

:::caution
Newer versions of toolkits dropped support for Scala 2.12
:::

# Scala Toolkit

[Scala Toolkit](https://github.com/scala/toolkit) is an ongoing
effort by Scala Center and VirtusLab to compose a set of approachable libraries to solve
everyday problems.

You can easily add it to your Scala CLI project with the `--toolkit` option:

<ChainedSnippets>

```scala title=UseOsLib.sc
println(os.pwd)
```

```bash
scala-cli UseOsLib.sc --toolkit default
```

</ChainedSnippets>

Similarly, you can achieve the same with the `using toolkit` directive:

```scala compile
//> using toolkit default
@main def printPwd: Unit = println(os.pwd)
```

## Scala Toolkit and tests

Adding Scala Toolkit to your project effectively adds 2 dependencies to your classpath:

- `org.scala-lang:toolkit:<version>` for your main scope (usable everywhere in the project);
- `org.scala-lang:toolkit-test:<version>` for your test scope (usable only in tests).

`toolkit-test` includes a batch of libraries only relevant for testing (like i.e. `munit`), which you probably don't
want on your main scope
class path (which is why Scala CLI won't put it there).
And so, you can use it like this:

<ChainedSnippets>

```scala title=Something.test.scala
//> using toolkit default
class Something extends munit.FunSuite {
  test("foo") {
    assert(true)
  }
}
```

```bash
scala-cli test Something.test.scala
```

</ChainedSnippets>

Also, in case you only want Scala Toolkit to be added to the test scope (and not for the main scope in any capacity),
you can always use the `using test.toolkit` directive.

<ChainedSnippets>

```scala title=project.scala
//> using test.toolkit default
```

```scala title=Another.test.scala
class Another extends munit.FunSuite {
  test("foo") {
    assert(os.pwd.last.nonEmpty)
  }
}
```

```bash
scala-cli test Another.test.scala project.scala
```

</ChainedSnippets>

More details about test scope directives can be found in
the [`using` directives guide](using-directives.md#directives-with-a-test-scope-equivalent).

## Other toolkits

Scala CLI also supports adding other toolkits to your project in a similar manner. Those have to follow the same
structure of 2 dependencies with the names `toolkit` and `toolkit-test`.
To do so, you have to explicitly pass the organisation the toolkit was released under (or an alias if defined).

For example, to add the [Typelevel Toolkit](https://github.com/typelevel/toolkit) to your project, you can pass it with
the `--toolkit` option:

<ChainedSnippets>

```scala title=UseTypelevel.scala
import cats.effect.*
import fs2.io.file.Files

object Hello extends IOApp.Simple {
  def run = Files[IO].currentWorkingDirectory.flatMap { cwd =>
    IO.println(cwd.toString)
  }
}
```

```bash
scala-cli UseTypelevel.scala --toolkit org.typelevel:default
scala-cli UseTypelevel.scala --toolkit typelevel:default # typelevel has a shorter alias defined
```

</ChainedSnippets>

Similarly, you can achieve the same with the `using toolkit` directive:

```scala compile
//> using toolkit org.typelevel:default

import cats.effect.*
import fs2.io.file.Files

object Hello extends IOApp.Simple {
  def run = Files[IO].currentWorkingDirectory.flatMap { cwd =>
    IO.println(cwd.toString)
  }
}
```

Or with the alias:

```scala compile
//> using toolkit typelevel:default

import cats.effect.*
import fs2.io.file.Files

object Hello extends IOApp.Simple {
  def run = Files[IO].currentWorkingDirectory.flatMap { cwd =>
    IO.println(cwd.toString)
  }
}
```




