---
title: Introduction
---

Scala CLI is a command line tools that execute given command on provided [inputs](/docs/guides/input) with given [configuration](/docs/guides/configuration) to produce a result. Most important commands are:

  - [compile](#a) to compile you code (this exclude tests)
  - [run](#a) (also used when no other command is provided) - run your code using provided arguments
  - [test](#a) - compile and run tests defined in your code
  - [package](#a) - package your code into a jar or other format so it can be shipped
  - [repl](#a) / [console](#a) - run interactive Scala shell
  - [fmt] to format your code



Since most of the tasks requires compilation or dependency resolution under the hood, Scala CLI heavily use caches and incremental compilation under the hood to provide output as quickly as possible. 

To ensure the quickest compilation, Scala CLI uses and manages [Bloop](https://scalacenter.github.io/bloop/) server. We have a [detailed guide](/docs/reference/bloop) how Scala CLI interacts with local bloop server.

Scala CLI uses [coursier](https://get-coursier.io/) to manage dependecies and share caches with other tools using Coursier (like `sbt` or `mill`)



