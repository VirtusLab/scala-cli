---
title: Sharing and testing code with GitHub gists
sidebar_position: 8
---

import {ChainedSnippets} from "../../src/components/MarkdownComponents.js";

## Running code from gists

Scala CLI lets you run Scala code straight from GitHub gists, without the need to manually download them first.
This is done by passing the link to a gist as an argument to Scala CLI:

For example, given the gist `https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec`, which contains these two files:
```scala title=Messages.scala
object Messages {
  def hello = "Hello"
}
```
```scala title=run.sc
println(Messages.hello)
```

You can run them with Scala CLI like this:
```bash
scala-cli https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec
```
<!-- Expected:
Hello
-->

This example prints `Hello` to the standard output.

:::note
As shown in this example, the gist isn't limited to just one file.
Scala CLI downloads the gist's archive and unzips it, so the gist can contain multiple files that depend on each other.

Scala CLI also caches the project sources using Coursier's cache.
:::

## Sharing code snippets

Together with the GitHub CLI (`gh`), it becomes really easy to share Scala code.
If you want to share a code file named `file.scala`, just run this command to create the gist:

```sh
gh gist create file.scala
```

Then you (and others) can run it quickly, using the Scala CLI approach shown above.


## Resources from gists

You can also use resources from gists archive. This is done by passing `resourceDir` in using directives.

For example, given the gist `https://gist.github.com/lwronski/7ee12fa4b8b8bac3211841273df82080` which containing Scala code and text file:

```scala title=Hello.scala
//> using resourceDir "./"
import scala.io.Source

object Hello extends App {
    val inputs = Source.fromResource("input").getLines.map(_.toInt).toSeq
    println(inputs.mkString(","))
}
```

```scala title=input
1
2
3
4
```

and run them:

<ChainedSnippets>

```bash
scala-cli https://gist.github.com/lwronski/7ee12fa4b8b8bac3211841273df82080
```

```text
1,2,3,4
```

</ChainedSnippets>

<!-- Expected:
1,2,3,4
-->

it will print `1,2,3,4` to the standard output.

## Gists and Markdown code

:::note
This feature is a work in progress and should currently be treated as experimental.
Markdown sources are ignored by default unless passed explicitly as inputs.
You can enable including non-explicit `.md` inputs by passing the `--enable-markdown` option.
:::

It is possible to run markdown sources from a GitHub gist. 
The gist is technically treated as a zipped archive (which it is downloaded as), so it is necessary to pass
the `--enable-markdown` option alongside the gist URL to run any contained Markdown sources.

<ChainedSnippets>

```bash
scala-cli --power https://gist.github.com/Gedochao/6415211eeb8ca4d8d6db123f83f0f839 --enable-markdown
```

```text
Hello
```

</ChainedSnippets>

You can find more information on working with Markdown in the [Markdown guide](/docs/guides/markdown.md).
