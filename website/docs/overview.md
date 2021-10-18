---
title: Overview
sidebar_position: 1
---

The `scala-cli` CLI aims at making it easier to run, test, and package, Scala sources.

It can:
- [compile](/docs/comamnds/compile) Scala code,
- [run](/docs/comamnds/run) it,
- [package](/docs/comamnds/package) it as a JAR or in formats such as deb, rpm, MSI, …,
- fire up a [REPL](/docs/comamnds/repl) allowing you to quickly play with the code,
- compile and run [tests](/docs/comamnds/test) suites,

... and many other things.

Scala CLI supports most recent Scala versions (`3.x`, `2.13.x` and `2.12.x`) and changing the Scala version as easy as providing `--scala` parameter (more in [the cookbook](#/docs/cookbooks/scala-versions)). 

Scala CLI supports as well compiling and running Scala to JVM (by default), [Scala.js](/docs/guides/scala-js) and [Scala Native](/docs/guides/scala-native).

## Installation

import BasicInstall from "../src/components/BasicInstall"

<BasicInstall/>

Prefer some other way to install Scala CLI? Head out to [Advanced installaion guide](/install#advanced-installation)


## What next?

Scala-cli documentation is split into 3 main section:
 - [Commands](/docs/commands/input) where you learn how to use most important commands that Scala CLI offers
 - [Guides](/docs/guides/ide) where you can read about core aspects of Scala CLI and learn how Scala CLI interacts with other tools like IDE
 - Scala CLI [Cookbook](/cookbooks/index) where you can learn how to solve particular problems with Scala CLI

**Happy hackking with Scala CLI!**

![Demo](/img/dark/demo.svg)