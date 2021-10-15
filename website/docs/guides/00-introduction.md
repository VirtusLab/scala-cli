---
title: Introduction
---

Scala CLI is a command line tools that execute given command on provided [inputs](/docs/guides/input) with given [configuration](/docs/guides/configuration) to produce a result. Most important commands are:

  - [compile](/docs/guides/compile) to compile you code (this exclude tests)
  - [run](/docs/guides/run) - to run your code using provided arguments (also used when no other command is provided)
  - [test](/docs/guides/test) - to compile and run tests defined in your code
  - [package](/docs/guides/package) - to package your code into a jar or other format
  - [repl](/docs/guides/repl) / [console](/docs/guides/repl) - to run interactive Scala shell
  - [fmt](/docs/guides/fmt) - to format your code

Scala CLI can be run without any command provided and that will default to the `run` command, so `scala-cli a.scala` will run your a.scala file.

Scala CLI supports most recent Scala versions (3.x, 2.13.x and 2.12.x) and changing the Scala version as easy as providing `--scala` parameter (more in [the cookbook](#/docs/cookbooks/scala-versions)). 

Scala CLI supports as well compiling and running Scala to JVM (by default), [Scala.js](/docs/guides/scala-js) and [Scala Native](http://localhost:3000/docs/guides/scala-native).

## Under the hood

### Caching and incrementality

Since most of the tasks requires compilation or dependency resolution under the hood, Scala CLI heavily use caches and incrementality under the hood to provide output as quickly as possible. Incremental compilation or caching are not perfect. In some cases, we need to be 100% that our problems are caused by our code (or e.g. bug in compiler) not the stale state of our project. For that reason we have introduce [clean](/docs/guides/clean) command that invalidates local caches and forces next compilation to be complete (non-incremental).

In cases something goes wrong withing incremental compilation or caching or when one would be 100% sure that the problems are caused by code not under-compilation.

We provide more in-depth overview about our caching in the guide dedicated to [Scala CLI internals](/docs/guides/internals).
### Bloop and coursier

To ensure the quickest compilation, Scala CLI uses and manages [Bloop](https://scalacenter.github.io/bloop/) compilation server. We have a [detailed guide](/docs/reference/bloop) how Scala CLI interacts with local bloop server.

Scala CLI uses [coursier](https://get-coursier.io/) to manage dependecies and share caches with other tools using Coursier (like `sbt` or `mill`).



