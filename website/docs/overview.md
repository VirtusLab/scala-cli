---
title: Overview
sidebar_position: 1
---

The `scala-cli` CLI makes it easier to compile, run, test, and package Scala code.

It can:
- [compile](./commands/compile.md) Scala code
- [run](./commands/run.md) it
- [package](./commands/package.md) it as a JAR file, or in formats such as deb, rpm, MSI, ...
- fire up a [REPL](./commands/repl.md), letting you quickly play with the code
- compile and run [tests](./commands/test.md) suites

... and more!

Scala CLI supports most recent Scala versions (`3.x`, `2.13.x` and `2.12.x`), and changing the Scala version as easy as providing the `--scala` parameter. (See [the cookbook](./cookbooks/scala-versions.md) for more information.)

As well as compiling and running Scala code with the JVM (the default), Scala CLI also supports [Scala.js](./guides/scala-js.md) and [Scala Native](./guides/scala-native.md).

## Installation

import BasicInstall from "../src/components/BasicInstall"

<BasicInstall/>

Prefer another way to install Scala CLI? See our [Advanced installation guide](/install#advanced-installation).


## What’s next?

Scala-CLI documentation is split into three main sections:
- [Getting started](./getting_started.md), where you learn how to start with Scala CLI
- [Commands](./commands/basics.md), where you learn the most important Scala CLI commands
- [Guides](./guides/intro.md), where you can read about the core aspects of Scala CLI, and learn how Scala CLI interacts with other tools, like your IDE
- Scala CLI [Cookbook](./cookbooks/intro.md), where you can learn how to solve specific problems with Scala CLI

**Happy hacking with Scala CLI!**

![Demo](/img/dark/demo.gif)

