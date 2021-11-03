---
title: Sharing and testing code with GitHub gists
sidebar_position: 6
---

## Running code from gists

`scala-cli` lets you run Scala code straight from GitHub gists, without the need to manually download them first.
This is done by passing the link to a gist as an argument to `scala-cli`:

For example, given the gist `https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec`, which contains these two files:
```scala title=Messages.scala
object Messages {
  def hello = "Hello"
}
```
```scala title=run.sc
println(Messages.hello)
```

You can run them with `scala-cli` like this:
```bash
scala-cli https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec
```
<!-- Expected:
Hello
-->

This example prints `Hello` to the standard output.

:::note
As shown in this example, the gist isn't limited to just one file.
`scala-cli` downloads the gist's archive and unzips it, so the gist can contain multiple files that depend on each other.

`scala-cli` also caches the project sources using Coursier's cache.
:::

## Sharing code snippets

Together with the GitHub CLI (`gh`), it becomes really easy to share Scala code.
If you want to share a code file named `file.scala`, just run this command to create the gist:

```sh
gh gist create file.scala
```

Then you (and others) can run it quickly, using the `scala-cli` approach shown above.
